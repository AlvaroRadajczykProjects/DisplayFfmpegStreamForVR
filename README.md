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

## Prepare ffmpeg advices

1. Use hardware with any hardware video encoder component (e.g. any laptop with HEVC/H265 hardware video encoder). Audio needs less resources and can be encoded via software
2. Run ffmpeg in non full free software OS (Fedora may be a trouble if HEVC hardware support is a must). Ubuntu containers may be sufficient
3. Screen recording needs an specific service running (e.g. v4l2loopback in any Linux distribution)

Here's an ffmpeg streamming example command (using VAAPI):

    ffmpeg -re -threads 2 -async 1 -vsync passthrough -probesize 32 -analyzeduration 0 -flags +low_delay -fflags +nobuffer -fflags +genpts -fflags +igndts -max_delay 0 -max_probe_packets 1 -flush_packets 1 -rtbufsize 2M -hwaccel vaapi -hwaccel_output_format yuv420p -vaapi_device /dev/dri/renderD128 -f v4l2 -i /dev/video2 -f alsa -i default -vf 'format=nv12,hwupload' -c:v hevc_vaapi -b:v 2M -c:a libfdk_aac -b:a 128k -preset ultrafast -qp 0 -g 4 -copytb 1 -af aresample=async=1 -f mpegts udp://DEVICE_IP_ADDRESS:1234

## How to listen to a ffmpeg stream

This app will listen to ffmpeg video and audio streamming via udp protocol within same network at this device ipv4 address and 1234 port. 

First ffmpeg process streamming via udp must be running. Then is possible to watch it from this app at streamming mode if both devices are connected at same network and receiver can listen to sender
