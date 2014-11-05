package io.github.greetlib.greetbot

import io.github.greetlib.greet.event.IRCEventListener
import io.github.greetlib.greetbot.listener.CommandHandler
import io.github.greetlib.greetbot.model.CommandDefinition
import io.github.greetlib.greetbot.model.ModuleData


abstract class GreetBotModule implements CommandHandler, IRCEventListener {
    GreetBot greetBot = GreetBot.getInstance()
    final ArrayList<CommandDefinition> commands = new ArrayList<>()
    void onCommand(Command command) {    }

    abstract ModuleData getModuleData()
}
