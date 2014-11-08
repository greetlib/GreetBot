package io.github.greetlib.greetbot.modules

import groovy.util.logging.Log4j2
import io.github.greetlib.greet.IRCConnection
import io.github.greetlib.greetbot.GreetBotModule
import io.github.greetlib.greetbot.model.ModuleData

@Log4j2
class UpdateModule extends GreetBotModule {
    ModuleData moduleData = [
            name: 'Update',
            version: '0.1a',
            author: 'GreetBot',
            shortDescription: 'Automatic updates via git repository',
            tokenID: 1
    ]
    private String lastBadRevision
    private String lastGoodRevision

    TimerTask updateTask = new TimerTask() {
        @Override
        void run() {
            log.info "Checking for updates..."
            String currentRevision = execute("git rev-parse --short HEAD")
            lastGoodRevision = currentRevision
            execute("git fetch")
            String newRevision = execute("git rev-parse --short origin")
            if(newRevision == lastBadRevision) {
                log.info "Revision has previously failed testing. Not updating."
                return
            }
            if(currentRevision == newRevision) {
                log.info "No updates."
                return
            }
            execute("git checkout origin/HEAD")
            String branch = execute("git symbolic-ref -q --short HEAD")
            broadcastMessage "Starting update from $branch branch revision $currentRevision->$newRevision. Running tests."
            ArrayList<String> testResults = execute("gradle check | tail -n 3 | grep '.'").split("\n")
            String testTime = testResults[1].substring(testResults[1].indexOf(":"))
            if(testResults[0] == "BUILD SUCCESSFUL") {
                broadcastMessage "Tests passed in ${testTime}. Restarting for update to revision $newRevision"
                log.info "Updating to revision ${newRevision}"
                greetBot.stop()
            }
            else {
                broadcastMessage "Tests failed. Staying on revision $currentRevision"
                lastBadRevision = newRevision
                log.info "Tests failed for revision $newRevision"
                execute("git checkout $currentRevision")
            }
        }
    }

    public broadcastMessage(String msg) {
        greetBot.conMap.values().each {
            IRCConnection con = it
            it.channelInfoMap.each {
                con.sendMessage it.value.channelName, msg
                sleep 500
            }
        }
    }

    private String execute(String c) {
        ArrayList<String> cmd = ["/bin/bash", '-c', c]
        log.debug cmd
        StringBuffer output = [""]
        Process p = cmd.execute()
        p.waitForProcessOutput(output, output)
        log.debug output
        return output
    }

    public UpdateModule() {
        new Timer().scheduleAtFixedRate(updateTask, 0, 1000*60*5)
    }

    @Override
    ModuleData getModuleData() {
        return moduleData
    }
}
