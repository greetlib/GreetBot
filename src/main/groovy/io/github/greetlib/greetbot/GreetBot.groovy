package io.github.greetlib.greetbot

import groovy.util.logging.Log4j2
import io.github.greetlib.greet.IRCConnection
import io.github.greetlib.greet.event.EventHandler
import io.github.greetlib.greet.event.EventManager
import io.github.greetlib.greet.event.IRCEventListener
import io.github.greetlib.greet.event.irc.MOTDEndEvent
import io.github.greetlib.greet.net.ClientInfo
import io.github.greetlib.greetbot.config.BotConfig
import io.github.greetlib.greetbot.database.DatabaseConnection
import io.github.greetlib.greetbot.database.SQLDatabaseConnection
import io.github.greetlib.greetbot.listener.CommandManager
import io.github.greetlib.greetbot.model.ChannelData
import io.github.greetlib.greetbot.modules.AuthenticationModule
import io.github.greetlib.greetbot.modules.CacheUtilityModule
import io.github.greetlib.greetbot.modules.ChannelModule
import io.github.greetlib.greetbot.modules.EvalModule
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Log4j2
class GreetBot implements IRCEventListener {
    private static GreetBot instance
    final HashMap<String, IRCConnection> conMap = new HashMap<>()
    final ArrayList<EventManager> eventManagers = new ArrayList<>()

    private DatabaseConnection database
    BotConfig botConfig;
    final CommandManager commandManager

    ExecutorService threadPool = Executors.newCachedThreadPool()

    private GreetBot() {
        instance = this
        botConfig = BotConfig.load("config.yml")
        database = new SQLDatabaseConnection(botConfig)
        syncConfigWithDatabase()
        commandManager = new CommandManager()
    }

    private void start() {
        for(BotConfig.ServerConfig server : botConfig.serverConfigMap.values()) {
            threadPool.submit {
                final ClientInfo clientInfo = [
                        userName: server.user,
                        nickName: server.nick,
                        realName: server.name,
                        password: server.pass,
                        networkAlias: server.alias
                ]
                IRCConnection con = new IRCConnection(clientInfo)
                eventManagers.add(con.eventManager)
                con.eventManager.addListener(this)
                con.eventManager.addListener(commandManager)
                conMap.put(server.alias, con)
                ChannelFuture f = con.connect(server.host, server.port)
                Channel c = f.channel()
                f.sync()
                c.closeFuture().sync()
            }
        }
    }

    private syncConfigWithDatabase() {
        for(BotConfig.ServerConfig server : botConfig.serverConfigMap.values()) {
            database.addNetwork(server.alias)
            ChannelData channelData = [
                    networkAlias: server.alias,
                    channelName: server.channel,
                    adminTokenID: 1,
                    commandPrefix: server.prefix
            ]
            database.addChannel(channelData)
        }
    }

    @EventHandler
    public onConnect(MOTDEndEvent event) {
        // Make sure no modules are loaded until all event handlers are initialized
        if(eventManagers.size() == botConfig.serverConfigMap.size()) {
            for(IRCConnection con : conMap.values()) {
                String networkAlias = con.clientInfo.networkAlias
                database.getChannelNames(networkAlias).each {
                    con.join(it)
                }
            }
            ModuleLoader.loadModule(AuthenticationModule.class, true)
            ModuleLoader.loadModule(EvalModule.class, true)
            ModuleLoader.loadModule(CacheUtilityModule.class, true)
            ModuleLoader.loadModule(ChannelModule.class, true)
        }
    }

    static GreetBot getInstance() {
        return instance
    }

    static void main(String[] args) {
        GreetBot greetBot = new GreetBot()
        greetBot.start()
    }

    public DatabaseConnection getDatabase() {
        return database
    }
}
