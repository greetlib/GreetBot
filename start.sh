#!/bin/sh
createJar() {
    `eval gradle shadowJar > /dev/tty`
}

createJar
runCommand="java -Xmx64m -Xmn64m -jar build/libs/greetbot-1.0-all.jar"
until `eval ${runCommand} > /dev/tty`; do
        echo "Restarting..."
        createJar
        sleep 1
done