# Project Title

Use the RTD1295 based Zidoo X8 / X9s to record any incoming HDMI to USB and broadcast via network (UDP, TS, H264).

The idea is to automatically capture any input to a USB stick. The main use case is in a presentation scenario to simply grab all presented content.

The Zidoo X8 and x9s are cheap devices (100 USD / 150 USD) and can capture 4k and 1080p HDMI input as H264 and also sent the input as UDP stream to a network. Both features are normally features of quite expensive hardware.

## Getting Started

This will describe how to setup a Zidoo X8 or X9s for remote debugging and install the software.

### Install steps
 1. RTD1295 based Android box. Tested with Zidoo X8 and Zidoo X9s.
 2. Upgrade firmware to version `1.4.12`
 3. [Root the device](https://www.zidoo.tv/Support/guide/guide_target/b7ywTwiBwahKKmVViAFMcQ%3D%3D.html)
 4. Install SSH server. I am using [SSHelper](https://arachnoid.com/android/SSHelper/).
    * I have the following settings
      * `Run SSHelper service at boot` = Yes
      * `Check Network connectivity` = No
      * `Allow voice messages` = No
 5. (optional) Install [BusyBox](https://f-droid.org/en/packages/ru.meefik.busybox/). Very helpful for local task on the device in the shell.
 6. SSH onto the device and activate remote debugging.
```
su
setprop service.adb.tcp.port 5555
stop adbd
start adbd
```
 7. From the local computer start adb session.
```
adb connect <IP address>:5555
```
 8. The application can now be developed from the Android Studio.
 9. The application must be deployed as system application. I was not able to write the recorded file onto the USB stick without system permissions.
```
adb push net.schmidtie.PresentationRecording.apk /sdcard/
adb shell
	su
	mount -o rw,remount /system
	mv /sdcard/net.schmidtie.PresentationRecording.apk /system/priv-app/
	chmod 644 /system/priv-app/net.schmidtie.PresentationRecording.apk

	am broadcast android.intent.action.ACTION_SHUTDOWN && sleep 5 && reboot -p
```

The installation is done by USB stick. I copy the image and all APK onto a stick.

## Issues
The following steps helped me to resolve HDMI glowing issues:
 * Change HDMI cable.
 * Adjust settings on Zidoo Box.
   * Picture Settings => Constract form 32 increase to 36

The following steps helped me to resolve fuzzy HDMI output:
 * Change HDMI input resolution to 1920x1080.

## Measurements

I have measued the display delay when using the Zidoo Box. The following measurements are an average of 5 measurements:
```
GoPro -(HDMI)-> TV
   163ms
GoPro -(HDMI)-> Zidoo -(HDMI)-> TV
   216ms
GoPro -(HDMI)-> Zidoo -(UDP)--> VLC (:network-caching=250)
   485ms
GoPro -(HDMI)-> Zidoo -(UDP)--> VLC (:network-caching=1000)
  1218ms
```
VLC network cache below 250ms was not working.

The computer running VLC used a USB 3.0 Gigabit network adapter.

## Internals

### Zidoo Online Updater

Source Urls:
 * China
   * `http://oldota.zidootv.com/index.php?m=Ota`
 * World
   * `http://ota.zidoo.tv/index.php?m=Ota`
   * `http://ota.zidootv.com/index.php?m=Ota`

Submitted information are:
 * Mac address
 * current Firmware version
 * Model name
 * Language

Example Url:
```
http://ota.zidoo.tv/index.php?m=Ota&mac=FF:FF:FF:FF:FF:FF&firmware=1.4.2&model=ZIDOO%20X9S&lang=en
```

Image is then downladed to `/cache/onlineupdate.zip`