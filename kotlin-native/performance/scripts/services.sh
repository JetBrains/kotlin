#!/usr/bin/env bash
# Command services.sh -mode [on|off] -gui file_with_servers_list.
MODE=ON
CHANGE_GUI_MODE=0
if [ "$1" = "-mode" ]; then
    shift
    if [ "$1" = "off" ]; then
        MODE=OFF
    fi
    shift
fi
if [ "$1" = "-gui" ]; then
    CHANGE_GUI_MODE=1
    shift
fi

INPUT_FILE=$1
if [ ! -f $INPUT_FILE ]; then
    echo "File $INPUT_FILE doesn't exist."
    exit 1
fi

if [ $CHANGE_GUI_MODE = 1 ]; then
    # Turn on/off GUI mode.
    if [ "$MODE" = ON ]; then
        systemctl enable graphical.target --force
        systemctl set-default graphical.target
    else
        systemctl enable multi-user.target --force
        systemctl set-default multi-user.target
    fi
fi

while IFS=$'\r' read -r line || [[ -n "$line" ]]; do
    if [ "$MODE" = ON ]; then
        systemctl enable $line
        systemctl start $line
    else
        systemctl disable $line
        systemctl stop $line
    fi
done < "$INPUT_FILE"
