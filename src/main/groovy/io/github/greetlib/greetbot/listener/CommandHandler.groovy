package io.github.greetlib.greetbot.listener

import io.github.greetlib.greetbot.Command

interface CommandHandler {
    void onCommand(Command command)
}
