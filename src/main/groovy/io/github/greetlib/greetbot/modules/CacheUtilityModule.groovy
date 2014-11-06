package io.github.greetlib.greetbot.modules

import io.github.greetlib.greetbot.Command
import io.github.greetlib.greetbot.GreetBotModule
import io.github.greetlib.greetbot.model.CommandDefinition
import io.github.greetlib.greetbot.model.ModuleData

class CacheUtilityModule extends GreetBotModule {
    private ModuleData moduleData = [
            tokenID: 1,
            name: 'CacheUtility',
            version: '0.1a',
            author: 'GreetBot',
            shortDescription: 'Utility for manipulating database cache'
    ]

    public CacheUtilityModule() {
        commands.add new CommandDefinition([
            command: 'cache',
            help: 'Cache modification and statistics',
            argCount: 1..1,
            usage: 'cache <stats|clear>'
        ])
    }

    @Override
    ModuleData getModuleData() {
        return moduleData
    }

    @Override
    void onCommand(Command c) {
        switch(c.args[0]) {
            case "stats":
                    cacheStats(c);
                break
            case "clear":
                    cacheClear(c);
                break
        }
    }

    void cacheStats(Command c) {
        c.messageEvent.reply greetBot.database.cacheStatistics.toString()
    }

    void cacheClear(Command c) {
        if(c.userData.globalAccess.accessLevel < 50) {
            return
        }
        greetBot.database.clearCache()
        c.messageEvent.reply "Cache cleared."
    }
}
