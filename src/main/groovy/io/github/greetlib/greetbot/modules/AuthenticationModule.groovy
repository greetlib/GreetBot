package io.github.greetlib.greetbot.modules

import groovy.util.logging.Log4j2
import io.github.greetlib.greet.net.UserInfo
import io.github.greetlib.greetbot.AccessPrivilege
import io.github.greetlib.greetbot.Command
import io.github.greetlib.greetbot.GreetBotModule
import io.github.greetlib.greetbot.model.CommandDefinition
import io.github.greetlib.greetbot.model.ModuleData
import io.github.greetlib.greetbot.model.UserData
import io.github.greetlib.greetbot.util.HostUtil
import io.github.greetlib.greetbot.util.TokenUtil

@Log4j2
class AuthenticationModule extends GreetBotModule {
    ModuleData moduleInfo = [
            tokenID: 1,
            name: "Authentication",
            version: '0.1a',
            author: 'GreetBot',
            shortDescription: "Provides authentication based on user@host mask"
    ]

    public AuthenticationModule() {
        commands.add(new CommandDefinition([
                command: "login",
                argCount: 2..2,
                help: "Authenticate this hostname",
                usage: "login <email> <token>"
        ]))
        commands.add(new CommandDefinition([
                command: "logout",
                argCount: 0..0,
                help: "Deauthenticate this hostname",
                usage: "deauth"
        ]))
        commands.add(new CommandDefinition([
                command: "token",
                argCount: 1..2,
                help: "Regenerate user token",
                usage: "token <email> [nick]",
                privilege: [accessLevel: 50]
        ]))
        commands.add(new CommandDefinition([
                command: "access",
                argCount: 2..3,
                help: "Change a users access level",
                usage: "access <nick> [global|#channel] <level>",
                privilege: [accessLevel: 50]
        ]))
        commands.add(new CommandDefinition([
                command: "adduser",
                argCount: 3..4,
                help: "Add a new user, if [global|#channel] is not specified, the current channel is used.",
                usage: "adduser <nick> <email> [global|#channel] <level>",
                privilege: [accessLevel: 10]
        ]))
    }

    @Override
    void onCommand(Command c) {
        log.info "Got command event ${c.cmd}"
        switch(c.cmdDef.command) {
            case "login":
                login(c)
                break
            case "logout":
                logout(c)
                break
            case "token":
                token(c)
                break
            case "access":
                access(c)
                break
            case "adduser":
                addUser(c)
                break
        }
    }

    private void token(Command c) {
        UserData userData = greetBot.database.getUserDataByEmail(c.args[0])
        if(!userData) {
            c.messageEvent.reply "Could not find email in database."
            return
        }
        String newToken = TokenUtil.generate()
        String newTokenHash = TokenUtil.hash(newToken)
        greetBot.database.setToken(userData.tokenID, newTokenHash)
        c.messageEvent.reply "Token generated and saved to database."
        if(c.args[1]) { c.messageEvent.connection.sendMessage(c.args[1],
            "A new token has been generated for your user. " +
            "Use 'login ${c.args[0]} $newToken' to authenticate your hostname."
        )}
    }

    private void login(Command c) {
        if(!c.messageEvent.isPrivate) return
        UserData userData = greetBot.database.getUserDataByEmail(c.args[0])
        if(!userData) {
            c.messageEvent.reply "Invalid token or email"
            return
        }
        if(!greetBot.database.verifyToken(userData.tokenID, c.args[1])) {
            c.messageEvent.reply "Invalid token or email"
            return
        }
        greetBot.database.addKnownHost(userData, c.userHost)
        c.messageEvent.reply "Authorized."
    }

    private void logout(Command c) {
        UserData userData = greetBot.database.getUserDataByHostname(c.userHost)
        if(!userData) {
            c.messageEvent.reply "$c.userHost not found in database."
            return
        }
        greetBot.database.deleteKnownHost(userData, c.userHost)
        c.messageEvent.reply "Hostname deauthorized"
    }

    private void access(Command c) {
        if(c.args[1] == "global" && c.userData.globalAccess.accessLevel < 50) {
            c.messageEvent.reply "You do not have the authority to set global access levels."
            return
        }
        if(c.args[1] != "global") {
            if(c.userData.channelAccess.get(c.args[1])?.accessLevel < 10 && c.userData.globalAccess.accessLevel < 50) {
                c.messageEvent.reply "You do not have the authority to set access levels for ${c.args[1]}"
                return
            }
        }
        UserInfo userInfo = c.messageEvent.connection.getUserInfo(c.args[0])
        if(!userInfo) {
            c.messageEvent.reply "I don't see ${c.args[0]} here."
            return
        }
        UserData userData = greetBot.database.getUserDataByHostname(c.userHost)
        if(!userData) {
            c.messageEvent.reply "${c.args[0]} is not a registered user."
            return
        }
        if(c.args[1] != "global") {
            if(!greetBot.database.getChannelNames(c.messageEvent.connection.clientInfo.networkAlias).contains(c.args[1])) {
                c.messageEvent.reply "Channel ${c.args[1]} not found in database."
            }
            greetBot.database.setChannelAccess(userData, c.args[1], c.messageEvent.connection.clientInfo.networkAlias,
                    c.args[2].toShort())
            c.messageEvent.reply "Access level in ${c.args[1]} for ${c.args[0]} set to ${c.args[2]}"
        }
        else {
            greetBot.database.setGlobalAccess(userData, c.args[2].toShort())
            c.messageEvent.reply "Global access for ${c.args[0]} set to ${c.args[2]}"
        }
    }

    private void addUser(Command c) {
        String target
        if(!c.args.last().isNumber()) {
            c.messageEvent.reply "Invalid access level '${c.args.last()}'"
        }
        if(c.args.size() > 3) {
            if(c.args[2] == "global" && c.userData.globalAccess.accessLevel < 50) {
                c.messageEvent.reply "You do not have authority to set global access levels."
                return
            }
            if(c.args[2] != "global") {
                if(c.userData.channelAccess.getOrDefault(c.args[2], AccessPrivilege.DEFAULT).accessLevel < 10
                        && c.userData.globalAccess.accessLevel < 50) {
                    c.messageEvent.reply "You do not have authority to set access levels for ${c.args[2]}"
                    return
                }
                long channelID = greetBot.database.getChannelID(c.args[2], c.messageEvent.connection.clientInfo.networkAlias)
                if(!channelID) {
                    c.messageEvent.reply "I don't have any record of channel ${c.args[2]}"
                    return
                }
                target = c.args[2]
            }
            else target = c.args[2]
        }
        else if(c.userData.channelAccess.getOrDefault(c.messageEvent.destination, AccessPrivilege.DEFAULT).accessLevel < 10) {
            c.messageEvent.reply "You do not have authority to set access levels for ${c.messageEvent.destination}"
            return
        }
        else target = c.messageEvent.destination
        String userHost = HostUtil.getUserHostMask(c.messageEvent.connection.getUserInfo(c.args[0])?.hostname)
        if(!userHost) {
            c.messageEvent.reply "I don't know of any ${c.args[0]}"
        }
        UserData userData = greetBot.database.getUserDataByHostname(userHost)
        if(userData) {
            c.messageEvent.reply "Existing record in database for $userHost. Use 'access' command to modify access levels."
            return
        }
        userData = new UserData()
        if(target == "global") userData.globalAccess = [accessLevel: c.args.last().toShort()] as AccessPrivilege
        else userData.channelAccess.put(target, [accessLevel: c.args.last().toShort()] as AccessPrivilege)
        String token = TokenUtil.generate()
        String tokenHash = TokenUtil.hash(token)
        greetBot.database.addUserData(userData, tokenHash, c.args[1], c.messageEvent.connection.clientInfo.networkAlias)
        greetBot.database.addKnownHost(userData, userHost)
        c.messageEvent.connection.sendMessage(c.args[0], "You now have a user account on " +
                "${c.messageEvent.connection.clientInfo.nickName}. Your current host has been authorized for this account")
        c.messageEvent.connection.sendMessage(c.args[0], "Your token is ${token} - DO NOT SHARE THIS TOKEN!")
        c.messageEvent.connection.sendMessage(c.args[0], "To authenticate a new host use the 'login' command.")
        c.messageEvent.reply "Account created."
    }

    @Override
    ModuleData getModuleData() {
        return moduleInfo
    }
}
