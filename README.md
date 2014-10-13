gilgamesh
=========

Gilga Meshenger: Messaging in the Bluetooth Babylon!

Some notes on the implementation, aka the glorious hack of Bluetooth Device Names. This application was original based on the Android SDK BluetoothChat sample. It used insecure (unpaired) and secure (paired) Bluetooth RFComm sockets to allow for short messages to be sent between devices. The primary modification that this project has made has been to add support for a "Broadcast" mode, that uses the Bluetooth device name, that is public visible during the Discovery process, as the message transport itself. 

The design goals of this project are:

* A truly decentralised application that requires only Bluetooth connectivity and has no central user registry
* Incredible ease of use that ensures all "mesh" connectivity happens with as little user involvment as possible
* Ability to enable trust or reputation for specific users or devices you message with
* A very transient app that stores no data permanently
* Ability to share the app easily between devices
* A "fire and forget" mode, where the user can enter a message, put the phone in their pocket, and walk around and area and have it broadcast to all devices it encounters

Android app:

![alt](https://raw.githubusercontent.com/n8fr8/gilgamesh/master/screens/device-2014-10-06-165447.png =250x) ![alt](https://raw.githubusercontent.com/n8fr8/gilgamesh/master/screens/device-2014-10-09-120044.png =250x)

Gilamesh also supports Linux:
https://github.com/n8fr8/gilgamesh/blob/master/docs/gilgamesh.sh

The key innovations/hacks/revelations that led us to this point were:

* As of recent Android versions, you can call an API to set your the device's Bluetooth visibility to a very long time ~1 hour  
* You can dynamically change the Bluetooth device name, and it can be long - up to 248 bytes encoded as UTF-8
* That the first two things above could be wrapped mostly in API calls the user did not have to see or worry about

Finally, some thoughts on security, privacy and reputation:

* This app supports both a public broadcast mode, and a private, direct message mode. It is easy to use to both. The direct message mode is optionally secured and encrypted at the Bluetooth level if you have paired with the device/user you are connected with.
* Impersonation is combatted by simplified user id's to a short (6 character alphanumeric) value, based on the device's unique Bluetooth ID. This makes them speakable and easy to remember. If someone says "trust messages from A1BC99" then likely you will be able to rememember that.
* If you pair with a user (using standard Bluetooth pairing settings), their userid will be appended with a *, to make it even easier to know this is someone you should trust
* The app ONLY works in Bluetooth mode, so though is no confusion when it might be using 3G/4G, Wifi or some other mode, and possibly go through a centralised server 
* The code is open-source, very small, and the entire app is only 28kb making it easy to audit, test and share 
* We make it easy to "retweet" a message by long pressing on it, which enables reputation for something to be built up by multiple people resharing it. If the user has paired with the user, you will also see the * next to the name to further indicate trust. 
  
