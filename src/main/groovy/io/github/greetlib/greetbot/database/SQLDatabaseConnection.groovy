package io.github.greetlib.greetbot.database

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Log4j2
import io.github.greetlib.greetbot.AccessPrivilege
import io.github.greetlib.greetbot.GreetBotModule
import io.github.greetlib.greetbot.cache.LRUCache
import io.github.greetlib.greetbot.cache.LRUCacheStatistics
import io.github.greetlib.greetbot.config.BotConfig
import io.github.greetlib.greetbot.model.ChannelData
import io.github.greetlib.greetbot.model.ModuleData
import io.github.greetlib.greetbot.model.UserData
import io.github.greetlib.greetbot.util.TokenUtil

import java.sql.ResultSet

@Log4j2
class SQLDatabaseConnection extends DatabaseConnection {
    private Sql sql
    private final String schema
    private final String DATABASE_URL
    private final String DATABASE_USER
    private final String DATABASE_PASS

    LRUCache<String, UserData> hostnameUserDataCache = new LRUCache<>();
    HashMap<String, HashMap<String, ChannelData>> networkChannelCache = new HashMap<>()

    public SQLDatabaseConnection(String host, int port, String schema, String user, String password) {
        DATABASE_URL = "jdbc:mysql://$host:$port/$schema"
        DATABASE_USER = user
        DATABASE_PASS = password
        sql = Sql.newInstance(DATABASE_URL, DATABASE_USER, DATABASE_PASS)
        this.schema = schema
    }

    public SQLDatabaseConnection(BotConfig botConfig) {
        this(botConfig.databaseConfig.host,
                botConfig.databaseConfig.port,
                botConfig.databaseConfig.schema,
                botConfig.databaseConfig.username,
                botConfig.databaseConfig.password)
    }

    private boolean checkConnection() {
        if(!sql.connection.isValid(2000)) {
            try {
                sql.close()
                sql = Sql.newInstance(DATABASE_URL, DATABASE_USER, DATABASE_PASS)
                if(sql.connection.isValid(2000)) return true
                else {
                    log.error "Could not connect to database"
                    return false
                }
            } catch(Exception ex) {
                log.error "Could not connect to database", ex
                return false
            }
        }
        return true
    }

    @Override
    synchronized UserData getUserDataByHostname(String hostname) {
        if(!checkConnection()) return null
        UserData userData = hostnameUserDataCache.get(hostname)
        if(!userData) {
            Long tokenID = getTokenIdByHostname(hostname)
            if(!tokenID) return null
            userData = [
                    tokenID: tokenID,
                    knownHosts: getKnownHosts(tokenID),
                    createdModules: getCreatedModules(tokenID),
                    channelAccess: getChannelAccess(tokenID),
                    globalAccess: getGlobalAccess(tokenID)
            ] as UserData
            hostnameUserDataCache.put hostname, userData
        }
        return userData
    }

    @Override
    UserData getUserDataByEmail(String email) {
        if(!checkConnection()) return null
        long tokenID = getTokenIDByEmail(email)
        if(!tokenID) return null
        [
            tokenID: tokenID,
            knownHosts: getKnownHosts(tokenID),
            createdModules: getCreatedModules(tokenID),
            channelAccess: getChannelAccess(tokenID),
            globalAccess: getGlobalAccess(tokenID)
        ] as UserData
    }

    @Override
    void saveModuleInfo(GreetBotModule greetBotModule) {
        if(!checkConnection()) return
        ModuleData moduleInfo = greetBotModule.moduleData
        sql.execute """
            INSERT INTO `Modules` (Name, Author, Version, TokenID, ShortDescription, LongDescription)
            VALUES(${moduleInfo.name}, ${moduleInfo.author}, ${moduleInfo.version}, ${moduleInfo.tokenID},
            ${moduleInfo.shortDescription}, ${moduleInfo.longDescription})
            ON DUPLICATE KEY UPDATE `Name` = `Name`
        """
        long moduleID = sql.firstRow(
                """SELECT `ModuleID` FROM `Modules` WHERE `Name` = ${moduleInfo.name}""")['ModuleID'] as long
        greetBotModule.commands.each {
            sql.execute """
                INSERT INTO `Commands` (`Name`, `Help`, `Usage`, `AccessLevel`, `ModuleID`)
                VALUES($it.command, $it.help, $it.usage, $it.privilege.accessLevel, $moduleID)
                ON DUPLICATE KEY UPDATE `Name` = `Name`
            """
        }
    }

    @Override
    void setToken(long tokenID, String tokenHash) {
        if(!checkConnection()) return
        sql.execute """
            UPDATE `Tokens` SET `Token` = $tokenHash WHERE `TokenID` = $tokenID
        """
    }

    @Override
    boolean verifyToken(long tokenID, String token) {
        if(!checkConnection()) return false
        String tokenHash = sql.firstRow("""
            SELECT `Token` FROM `Tokens` WHERE `TokenID` = $tokenID
        """)['Token']
        return TokenUtil.check(token, tokenHash)
    }

    @Override
    void addKnownHost(UserData userData, String userHost) {
        if(!checkConnection()) return
        sql.execute """
            INSERT INTO `Hosts` (TokenID, Host) VALUES(${userData.tokenID}, $userHost)
        """
        hostnameUserDataCache.put(userHost, userData)
        userData.knownHosts.add(userHost)
    }

    @Override
    void deleteKnownHost(UserData userData, String userHost) {
        if(!checkConnection()) return
        sql.execute """
            DELETE FROM `Hosts` WHERE `TokenID` = ${userData.tokenID} AND `Host` = ${userHost}
        """
        userData.knownHosts.remove(userHost)
    }

    @Override
    long getChannelID(String channelName, String networkAlias) {
        if(!checkConnection()) return null as Long
        long networkID = sql.firstRow("""
            SELECT `NetworkID` FROM `Networks` WHERE `Host` = $networkAlias
        """)['NetworkID'] as long
        long channelID = sql.firstRow("""
            SELECT `ChannelID` FROM `Channels` WHERE `NetworkID` = $networkID AND `ChannelName` = $channelName
        """)['ChannelID'] as long
        return channelID
    }

    @Override
    void setChannelAccess(UserData userData, String channelName, String networkAlias, short accessLevel) {
        if(!checkConnection()) return
        long channelID = getChannelID(channelName, networkAlias)
        sql.execute """
            REPLACE INTO `ChannelAccess` (TokenID, ChannelID, AccessLevel)
            VALUES(${userData.tokenID}, $channelID, $accessLevel)
        """
        userData.channelAccess.put(channelName, [accessLevel: accessLevel] as AccessPrivilege)
    }

    @Override
    void setGlobalAccess(UserData userData, short accessLevel) {
        if(!checkConnection()) return
        sql.execute """
            UPDATE `Tokens` SET `AccessLevel` = $accessLevel WHERE `TokenID` = ${userData.tokenID}
        """
        userData.globalAccess.accessLevel = accessLevel
    }

    @Override
    void addUserData(UserData userData, String tokenHash, String email, String networkAlias) {
        if(!checkConnection()) return
        sql.execute("""
            INSERT INTO `Tokens` (Token, Email, AccessLevel)
            VALUES($tokenHash, $email, ${userData.globalAccess.accessLevel})
            ON DUPLICATE KEY UPDATE `Token` = `Token`
        """)
        Long tokenID = sql.firstRow("""
            SELECT `TokenID` FROM `Tokens` WHERE `Token` = $tokenHash AND `Email` = $email
        """)?.get('TokenID') as Long
        if(tokenID == null) {
            log.error "Failed to get inserted Token ID"
            return
        }
        userData.tokenID = tokenID
        if(!userData.channelAccess.isEmpty()) {
            long networkID = getNetworkID(networkAlias)
            userData.channelAccess.each {
                long channelID = sql.firstRow("""
                    SELECT `ChannelID` FROM `Channels` WHERE `ChannelName` = ${it.key} AND `NetworkID` = $networkID
                """)['ChannelID'] as long
                sql.execute """
                    INSERT INTO `ChannelAccess` (ChannelID, TokenID, AccessLevel)
                    VALUES($channelID, $tokenID, ${it.value.accessLevel})
                """
            }
        }
    }

    @Override
    void addNetwork(String host) {
        if(!checkConnection()) return
        sql.execute """
            INSERT INTO `Networks` (Host) VALUES($host) ON DUPLICATE KEY UPDATE `Host` = `Host`
        """
    }

    @Override
    void addChannel(ChannelData channelData) {
        if(!checkConnection()) return
        long networkID = getNetworkID(channelData.networkAlias)
        boolean channelExists = sql.firstRow("""
            SELECT COUNT(*) AS `EXISTS` FROM `Channels` WHERE `ChannelName` = ${channelData.channelName} AND `NetworkID` = $networkID
        """)['EXISTS']
        if(channelExists) return
        sql.execute """
            INSERT INTO `Channels` (ChannelName, NetworkID, TokenID, CommandPrefix)
            VALUES(${channelData.channelName}, ${networkID}, ${channelData.adminTokenID}, ${channelData.commandPrefix})
        """
        long channelID = sql.firstRow("""
            SELECT `ChannelID` FROM `Channels` WHERE `NetworkID` = $networkID AND `ChannelName` = ${channelData.channelName}
        """)['ChannelID'] as long
        channelData.channelID = channelID
        if(networkChannelCache.get(channelData) == null) {
            networkChannelCache.put(channelData.networkAlias, new HashMap<String, ChannelData>())
        }
        networkChannelCache.get(channelData.networkAlias).put(channelData.channelName, channelData)
    }

    @Override
    void removeChannel(String channel, String networkAlias) {
        if(!checkConnection()) return
        long networkID = getNetworkID(networkAlias)
        sql.execute """
            DELETE FROM `Channels` WHERE `ChannelName` = ${channel} AND `NetworkID` = ${networkID}
        """
        networkChannelCache.get(networkAlias)?.remove(channel)
    }

    @Override
    ArrayList<String> getChannelNames(String networkAlias) {
        if(!checkConnection()) return null
        if(!networkChannelCache.containsKey(networkAlias) || networkChannelCache.get(networkAlias).isEmpty()) {
            long networkID = getNetworkID(networkAlias)
            HashMap<String, ChannelData> channelDataMap = new HashMap<>()
            sql.eachRow """
                SELECT `ChannelID`, `ChannelName`, `TokenID`, `CommandPrefix` FROM `Channels`
                WHERE `NetworkID` = $networkID
            """, { ResultSet it ->
                ChannelData channelData = [
                        networkAlias: networkAlias,
                        adminTokenID: it.getLong("TokenID"),
                        channelID: it.getLong("ChannelID"),
                        commandPrefix: it.getString("CommandPrefix"),
                        channelName: it.getString("ChannelName")
                ]
                channelDataMap.put channelData.channelName, channelData
            }
            networkChannelCache.put(networkAlias, channelDataMap)
        }
        ArrayList<String> channelNames = []
        networkChannelCache.get(networkAlias).values().each {
            channelNames << it.channelName
        }
        return channelNames
    }

    @Override
    ChannelData getChannelData(String networkAlias, String channelName) {
        if(!checkConnection()) return null
        if(!networkChannelCache.containsKey(networkAlias) || !networkChannelCache.get(networkAlias).containsKey(channelName)) {
            long networkID = getNetworkID(networkAlias)
            sql.eachRow """
                SELECT `ChannelID`, `ChannelName`, `TokenID`, `CommandPrefix` FROM `Channels`
                WHERE `NetworkID` = $networkID AND `ChannelName` = $channelName
            """, { ResultSet it ->
                ChannelData channelData = [
                        networkAlias: networkAlias,
                        adminTokenID: it.getLong("TokenID"),
                        channelID: it.getLong("ChannelID"),
                        commandPrefix: it.getString("CommandPrefix"),
                        channelName: it.getString("ChannelName")
                ]
                HashMap<String, ChannelData> channelDataMap = networkChannelCache.getOrDefault(networkAlias,
                        new HashMap<String, ChannelData>())
                channelDataMap.put(channelName, channelData)
                networkChannelCache.put(networkAlias, channelDataMap)
            }
        }
        return networkChannelCache.get(networkAlias).get(channelName)
    }

    @Override
    LRUCacheStatistics getCacheStatistics() {
        return hostnameUserDataCache.getCacheStats()
    }

    @Override
    void clearCache() {
        hostnameUserDataCache.clear()
    }

    private long getNetworkID(String alias) {
        if(!checkConnection()) return null as Long
        return sql.firstRow("""
                SELECT `NetworkID` FROM `Networks` WHERE `Host` = $alias
            """)?.get("NetworkID") as Long
    }

    private Long getTokenIdByHostname(String hostname) {
        GroovyRowResult result = sql.firstRow("SELECT `TokenID` from `Hosts` WHERE `Host` = $hostname")
        if(result == null) return null
        return result['TokenID'] as Long
    }

    private HashMap<String, AccessPrivilege> getChannelAccess(long tokenID) {
        if(!checkConnection()) return null
        HashMap<String, AccessPrivilege> channelAccess = new HashMap<>()
        sql.eachRow """
            SELECT `ChannelName`, `AccessLevel` from `ChannelAccess`, `Channels` WHERE `ChannelAccess`.`TokenID` = $tokenID
        """, { ResultSet it ->
            channelAccess.put it.getString("ChannelName"), [accessLevel: it.getLong("AccessLevel")] as AccessPrivilege
        }
        return channelAccess
    }

    private AccessPrivilege getGlobalAccess(long tokenID) {
        if(!checkConnection()) return null
        GroovyRowResult result = sql.firstRow("""
            SELECT `AccessLevel` from `Tokens` WHERE `TokenID` = $tokenID
        """)
        return [accessLevel: result["AccessLevel"]] as AccessPrivilege
    }

    private ArrayList<String> getKnownHosts(long tokenID) {
        if(!checkConnection()) return null
        ArrayList<String> knownHosts = []
        sql.eachRow """
            SELECT `Host` FROM `Hosts`
            WHERE `Hosts`.`TokenID` = $tokenID;
        """, { ResultSet it ->
            knownHosts << it.getString('Host')
        }
        return knownHosts
    }

    private ArrayList<String> getCreatedModules(long tokenID) {
        if(!checkConnection()) return null
        ArrayList<String> modules = new ArrayList<>()
        sql.eachRow """
            SELECT `Name` FROM `Modules`
            WHERE `TokenID` = $tokenID;
        """, { ResultSet it ->
            modules << it.getString("Name")
        }
        return modules
    }

    private long getTokenIDByEmail(String email) {
        if(!checkConnection()) return null as Long
        GroovyRowResult result = sql.firstRow """
            SELECT `TokenID` FROM `Tokens` WHERE `Email` = $email
        """
        return result ? ["TokenID"] as long : null
    }
}
