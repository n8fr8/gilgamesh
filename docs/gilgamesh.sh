#!/bin/bash

sudo bt-adapter --set Powered true
sudo bt-adapter --set Discoverable true
sudo bt-adapter --set DiscoverableTimeout 3600

while true; do
    read -p "What's happening nearby? " status
    if [ -z "$status" ];
    then
	echo "status not set"
    else
    	sudo bt-adapter --set Name " $status"
    fi
    echo "checking for local updates..."
    sudo bt-adapter -d
done
