package io.github.greetlib.greetbot.listener

import io.github.greetlib.greet.event.IRCEventListener
import io.github.greetlib.greetbot.GreetBot
import io.github.greetlib.greetbot.config.BotConfig


class GreetBotEventListener implements IRCEventListener {
    protected GreetBot greetBot
    protected BotConfig botConfig

    public GreetBotEventListener() {
        this.greetBot = GreetBot.instance
        this.botConfig = greetBot.botConfig
    }
}
