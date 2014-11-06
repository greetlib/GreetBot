package io.github.greetlib.greetbot.modules

import io.github.greetlib.greet.net.UserInfo
import io.github.greetlib.greetbot.Command
import io.github.greetlib.greetbot.GreetBotModule
import io.github.greetlib.greetbot.model.ChannelData
import io.github.greetlib.greetbot.model.CommandDefinition
import io.github.greetlib.greetbot.model.ModuleData
import io.github.greetlib.greetbot.util.HostUtil


class ChannelModule extends GreetBotModule {
    ModuleData moduleData = [
                name: 'Channel',
                version: '0.1a',
                author: 'GreetBot',
                shortDescription: 'Provides commands for channel management',
                tokenID: 1
    ]

    ChannelModule() {
        commands.add new CommandDefinition([
                command: 'join',
                argCount: 1..2,
                help: 'Join a channel. If [admin] is specified this channel will be saved to the database.',
                usage: 'join <channel> [admin]',
                privilege: [accessLevel: 50]
        ])
        commands.add new CommandDefinition([
                command: 'part',
                argCount: 1..2,
                help: "Leave a channel. If ['DELETE'] is specified this channel will be removed from the database.",
                usage: "part <channel> ['DELETE']",
                privilege: [accessLevel: 50]
        ])
    }

    @Override
    void onCommand(Command c) {
        switch(c.cmd) {
            case 'join':
                join(c)
                break
            case 'part':
                part(c)
        }
    }

    @Override
    ModuleData getModuleData() {
        return moduleData
    }

    //TODO These kinds of API calls are unacceptable. Top of the list.
    void join(Command c) {
        c.messageEvent.connection.join c.args[0]
        String reply = "Joined ${c.args[0]}. "
        if(c.args.size() == 2) {
            UserInfo adminUserInfo = c.messageEvent.connection.getUserInfo(c.args[1])
            String adminHost = HostUtil.getUserHostMask(adminUserInfo.hostname)
            long adminTokenID = greetBot.database.getUserDataByHostname(adminHost).tokenID
            greetBot.database.addChannel(new ChannelData([
                    networkAlias: c.messageEvent.connection.clientInfo.networkAlias,
                    channelName: c.args[0],
                    adminTokenID: adminTokenID
            ]))
            reply << "Channel saved to database."
        }
        c.messageEvent.reply reply
    }

    void part(Command c) {
        c.messageEvent.connection.part c.args[0]
        String reply = "Parted ${c.args[0]} "
        if(c.args.size() == 2) {
            greetBot.database.removeChannel(c.args[0], c.messageEvent.connection.clientInfo.networkAlias)
            reply << "Channel removed from database."
        }
        c.messageEvent.reply reply
    }
}
