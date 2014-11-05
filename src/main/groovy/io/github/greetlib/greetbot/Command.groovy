package io.github.greetlib.greetbot

import io.github.greetlib.greet.event.irc.MessageEvent
import io.github.greetlib.greet.net.ChannelInfo
import io.github.greetlib.greet.net.UserInfo
import io.github.greetlib.greetbot.model.CommandDefinition
import io.github.greetlib.greetbot.model.UserData
import io.github.greetlib.greetbot.util.HostUtil


class Command {
    MessageEvent messageEvent
    String cmd
    List<String> args
    CommandDefinition cmdDef
    UserInfo userInfo
    UserData userData
    ChannelInfo channelInfo
    String userHost

    public Command(String cmd, List<String> args, MessageEvent messageEvent, CommandDefinition cmdDef, UserData userData) {
        this.messageEvent = messageEvent
        this.cmd = cmd
        this.args = args
        this.messageEvent = messageEvent
        this.cmdDef = cmdDef
        this.userData = userData
        userInfo = messageEvent.connection.getUserInfo(messageEvent.source, true)
        if(!messageEvent.isPrivate) {
            channelInfo = messageEvent.connection.getChannelInfo(messageEvent.destination, true)
        }
        userHost = HostUtil.getUserHostMask(messageEvent.serverResponse.source)
    }
}
