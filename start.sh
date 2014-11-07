#!/bin/sh
until `gradle run > /dev/tty`; do
        echo "Restarting..."
        sleep 1
done
