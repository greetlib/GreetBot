#!/bin/sh
createJar() {
    `eval ./gradlew shadowJar > /dev/tty`
}

createJar
runCommand="java -Xmx64m -Xms64m -jar build/libs/greetbot-1.0-all.jar"
until `eval ${runCommand} > /dev/tty`; do
        echo "Restarting..."
        createJar
        sleep 1
done
