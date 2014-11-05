package io.github.greetlib.greetbot.model

import io.github.greetlib.greetbot.AccessPrivilege


class CommandDefinition {
    boolean isEnabled = true
    String command
    Range argCount
    String help
    String usage
    AccessPrivilege privilege = AccessPrivilege.DEFAULT
}
