package io.github.greetlib.greetbot

import io.github.greetlib.greetbot.exception.InvalidModuleMetadataException
import io.github.greetlib.greetbot.model.CommandDefinition
import io.github.greetlib.greetbot.model.ModuleData


class ModuleLoader {
    static HashMap<String, GreetBotModule> moduleMap = new HashMap<>()

    static loadModule(GreetBotModule module, boolean saveToDatabase = false) {
        String invalid = validateModule(module)
        if(invalid) {
            throw new InvalidModuleMetadataException(invalid)
        }
        GreetBot.instance.commandManager.registerModule(module)
        GreetBot.instance.eventManagers.each { it.addListener(module) }
        if(saveToDatabase) {
            GreetBot.instance.database.saveModuleInfo(module)
        }
    }

    static unloadModule(String name) {
        GreetBotModule module = moduleMap.get(name)
        if(!module) return
        GreetBot.instance.commandManager.unregisterModule(module)
        GreetBot.instance.eventManagers.each { it.removeListener(module) }
    }

    static loadModule(Class<GreetBotModule> moduleClass, boolean saveToDatabase = false) {
        GreetBotModule module = moduleClass.getConstructor().newInstance()
        loadModule(module, saveToDatabase)
    }

    static String validateModule(GreetBotModule module) {
        ModuleData moduleInfo = module.getModuleData()
        if(!moduleInfo) {
            return "Module has no metadata"
        }
        if(!moduleInfo.name || moduleInfo.name.length() > 30) {
            return "Module name missing or invalid"
        }
        if(!moduleInfo.author || moduleInfo.author.length() > 30) {
            return "Author name missing or invalid"
        }
        if(moduleInfo.shortDescription && moduleInfo.shortDescription.length() > 100) {
            return "Short description is too long"
        }
        if(!moduleInfo.version || moduleInfo.version.length() > 30) {
            return "Module version invalid or too long"
        }
        if(!moduleInfo.tokenID) {
            return "No administrative token ID specified."
        }
        // Check for command collisions
        ArrayList<String> cmds = []
        module.getCommands().each { cmds << it.command}
        CommandDefinition cCollide
        GreetBotModule mCollide = moduleMap.values().find {
            cCollide = it.commands.find { cmds.contains(it.command)}
        }
        if(cCollide) return "Collision with command '${cCollide.command}' defined in ${mCollide.moduleData.name}"
        return ""
    }
}
