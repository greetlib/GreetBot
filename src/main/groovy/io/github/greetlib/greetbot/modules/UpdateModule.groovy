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
    ]
    private String lastBadRevision
    private String lastGoodRevision

    TimerTask updateTask = new TimerTask() {
        @Override
        void run() {
            log.info "Checking for updates..."
            String currentRevision = "git rev-parse --short HEAD".execute().text
            lastGoodRevision = currentRevision
            "git fetch".execute()
            String newRevision = "git log origin --format=%h | head -n1".execute().text
            if(newRevision == lastBadRevision) {
                log.info "Revision has previously failed testing. Not updating."
                return
            }
            if(currentRevision == newRevision) {
                log.info "No updates."
                return
            }
            "git checkout origin/HEAD".execute()
            String branch = "git symbolic-ref -q --short HEAD".execute().text
            broadcastMessage "Starting update from $branch branch revision $currentRevision->$newRevision. Running tests."
            ArrayList<String> testResults = "gradle check | tail -n 3 | grep '.'".execute().text.split("\n")
            String testTime = testResults[1].substring(testResults[1].indexOf(": "))
            if(testResults[0] == "BUILD SUCCESSFUL") {
                broadcastMessage "Tests passed in ${testTime}. Restarting for update to revision $newRevision"
                log.info "Updating to revision ${newRevision}"
                System.exit(0) //TODO Graceful shutdown?
            }
            else {
                broadcastMessage "Tests failed. Staying on revision $currentRevision"
                lastBadRevision = newRevision
                log.info "Tests failed for revision $newRevision"
                "git checkout $currentRevision".execute()
            }
        }
    }

    public broadcastMessage(String msg) {
        greetBot.conMap.each {
            IRCConnection con = it.value
            it.value.channelInfoMap.values().each {
                con.sendMessage it.channelName, msg
                sleep 500
            }
        }
    }

    public UpdateModule() {
        new Timer().scheduleAtFixedRate(updateTask, 0, 1000*60*5)
    }

    @Override
    ModuleData getModuleData() {
        return moduleData
    }
}