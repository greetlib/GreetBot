package io.github.greetlib.greetbot.listener

import groovy.util.logging.Log4j2
import io.github.greetlib.greet.event.EventHandler
import io.github.greetlib.greet.event.irc.MessageEvent
import io.github.greetlib.greetbot.AccessPrivilege
import io.github.greetlib.greetbot.Command
import io.github.greetlib.greetbot.GreetBotModule
import io.github.greetlib.greetbot.model.ChannelData
import io.github.greetlib.greetbot.model.CommandDefinition
import io.github.greetlib.greetbot.model.UserData
import io.github.greetlib.greetbot.util.HostUtil

@Log4j2
class CommandManager extends GreetBotEventListener {
    static String DEFAULT_PREFIX = "#!"
    HashMap<String, CommandHandler> cmdMap = new HashMap<>()
    HashMap<String, CommandDefinition> cmdDefMap = new HashMap<>()

    @EventHandler
    void onMessage(MessageEvent event) {
        ChannelData channelData
        String prefix = ""
        if(!event.isPrivate) {
            channelData = greetBot.database.getChannelData(event.connection.clientInfo.networkAlias, event.destination)
            if(channelData) prefix = channelData.commandPrefix
            else prefix = DEFAULT_PREFIX
            if(!event.message.startsWith(prefix)) return
        }
        // Split into command and args
        List<String> cmdParts = event.message.split(" ")
        String cmd
        if(!prefix) cmd = cmdParts[0]
        else cmd = cmdParts[0].substring(prefix.size())
        CommandDefinition cmdDef = cmdDefMap.get(cmd)
        if(!cmdDef) return

        //TODO This all seems really messy, cleanup.
        // Call command handler
        String channel = event.destination.substring(1)
        String sourceHost = HostUtil.getUserHostMask(event.serverResponse.source)
        UserData userData = greetBot.database.getUserDataByHostname(sourceHost)
        boolean hasAccess
        AccessPrivilege accessPrivilege
        if(event.isPrivate) {
            if(!userData) accessPrivilege = AccessPrivilege.DEFAULT // USE DEFAULT
            else accessPrivilege = userData.globalAccess // USE GLOBAL
        }
        if(!event.isPrivate) {
            if(!userData) accessPrivilege = AccessPrivilege.DEFAULT // USE DEFAULT
            else if(userData.channelAccess.get(channel) == 0 && userData.globalAccess.accessLevel == 0) return // IGNORE USER
            else if(userData.globalAccess > userData.channelAccess.get(channel)) {
                accessPrivilege = userData.globalAccess
            }
            else accessPrivilege = userData.channelAccess.get(channel)
        }
        if(accessPrivilege == null) accessPrivilege = AccessPrivilege.DEFAULT
        if(accessPrivilege >= cmdDef.privilege) hasAccess = true
        if(hasAccess) {
            if(cmdDef.argCount && !cmdDef.argCount.contains(cmdParts.size()-1)) {
                event.reply "Syntax: ${cmdDef.usage}"
                return
            }
            try {
                Command command = new Command(cmd, cmdParts.subList(1, cmdParts.size()), event, cmdDef, userData)
                cmdMap.get(cmd).onCommand(command)
            } catch(Exception ex) {
                log.error "Error during command processing", ex
                event.reply "Error during command processing: ${ex.message}"
            }
        }
    }

    /**
     * Register a module. Calls {@link GreetBotModule#getCommands()} to retrieve {@link CommandDefinition}s
     * @param module
     */
    void registerModule(GreetBotModule module) {
        for(CommandDefinition cmd : module.getCommands()) {
            cmdDefMap.put(cmd.command, cmd)
            cmdMap.put(cmd.command, module)
        }
    }

    void unregisterModule(GreetBotModule module) {
        module.getCommands().each {
            cmdDefMap.remove(it.command)
            cmdMap.remove(it.command)
        }
    }
}
