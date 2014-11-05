package io.github.greetlib.greetbot.modules

import groovy.io.GroovyPrintWriter
import io.github.greetlib.greet.event.irc.MessageEvent
import io.github.greetlib.greetbot.Command
import io.github.greetlib.greetbot.GreetBotModule
import io.github.greetlib.greetbot.ScriptBase
import io.github.greetlib.greetbot.model.CommandDefinition
import io.github.greetlib.greetbot.model.ModuleData
import org.codehaus.groovy.control.CompilerConfiguration

class EvalModule extends GreetBotModule {
    Binding binding = new Binding();
    private CompilerConfiguration config = new CompilerConfiguration()
    private GroovyShell groovyShell

    ModuleData moduleData = [
            name: 'Eval',
            version: '0.1a',
            author: 'GreetBot',
            shortDescription: 'Run groovy scripts',
            tokenID: 1
    ]

    public EvalModule() {
        binding.setVariable("greetBot", greetBot)
        binding.setVariable("db", greetBot.database)
        config.setScriptBaseClass(ScriptBase.class.name)
        groovyShell = new GroovyShell(this.class.classLoader, binding, config)
        commands.add(new CommandDefinition([
            command: 'eval',
            argCount: 1..5000,
            help: 'Evaluate Groovy expression or short script',
            usage: 'eval <expression>',
            privilege: [accessLevel: 50]
        ]))
    }

    @Override
    ModuleData getModuleData() {
        return moduleData
    }

    void onCommand(Command c) {
        if(c.userData.globalAccess.accessLevel < 50) {
            c.messageEvent.reply "You do not have access to that command."
        }
        String exp = c.args.join(" ")
        String result = ""
        replyWriter.target = c.messageEvent
        try {
            result = groovyShell.evaluate(exp).toString()
        } catch(Exception ex) {
            result = ex.message
        }
        if(result.size() > 400) result = result.substring(0, 400)
        if(result.size() == 0) result = 'No output'
        c.messageEvent.reply "${result}"
    }
}
