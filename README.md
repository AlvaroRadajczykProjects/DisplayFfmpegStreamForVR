# DisplayFfmpegStreamForVR

Android receiving ffmpeg video and audio streamming in two horizontal views. It allows someone watch any ffmpeg udp video and audio streamming while lying on the bed with low cost VR headset

## Build and install on Android device

Just follow this instructions:
* Install Android Studio in your setup and open it
* Open this project
* Compile the app and launch it in your device

## How to use it

This app has two modes

* Camera mode(by default at beginning)
* Streamming mode

These are possible controls:

* Long press to turn on/off camera flash (when camera mode)
* Double tap to switch the current mode

## How to listen to a ffmpeg stream

This app will listen to ffmpeg video and audio streamming via udp protocol within same network at this device ipv4 address and 1234 port. 

First ffmpeg process streamming via udp must be running. Then is possible to watch it from this app at streamming mode if both devices are connected at same network and receiver can listen to sender
