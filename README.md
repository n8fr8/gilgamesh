gilgamesh
=========

Gilga Meshenger: King of Babylon!

The design goals of this project are:

* A truly decentralised application that requires only Bluetooth connectivity and has no central user registry
* Incredible ease of use that ensures all "mesh" connectivity happens with as little user involvment as possible
* Ability to enable trust or reputation for specific users or devices you message with
* A very transient app that stores no data permanently
* Ability to share the app easily between devices

Some notes on the implementation, aka the glorious hack of Bluetooth Device Names. This application was original based on the Android SDK BluetoothChat sample. It used insecure (unpaired) and secure (paired) Bluetooth RFComm sockets to allow for short messages to be sent between devices. The primary modification that this project has made has been to add support for a "Broadcast" mode, that uses the Bluetooth device name, that is public visible during the Discovery process, as the message transport itself. 

The key innovations/hacks/revelations that led us to this point were:

* As of recent Android versions, you can call an API to set your the device's Bluetooth visibility to a very long time ~1 hour  
* You can dynamically change the Bluetooth device name, and it can be long - up to 248 bytes encoded as UTF-8
* That the first two things above could be wrapped mostly in API calls the user did not have to see or worry about


