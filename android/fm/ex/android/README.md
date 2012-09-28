# IEXAudio for Android

A PhoneGap plugin to make HTML5 Audio work.  Instead of creating a new `Audio()` instance,
create a new `IEXAudio()` instance.


Under the hood, this is the playback code pulled out of our native
app.  It runs as a service and does as much as possible to stay
off the main UI thread (this will improve further in the future).  It also uses lots of intents to make it dead simple to implement
a homescreen widget as well.

## Setup

Add the following to your AndroidManifest.xml to register the playback service.

    <service android:name=".IEXAudioService">

            <intent-filter>
                <action android:name="fm.ex.android.setsource" />
                <action android:name="fm.ex.android.play" />
                <action android:name="fm.ex.android.stop" />
                <action android:name="fm.ex.android.seek" />
                <action android:name="fm.ex.android.pause" />
                <action android:name="fm.ex.android.load" />
                <action android:name="fm.ex.android.remotenext" />
                <action android:name="fm.ex.android.remoteprev" />
                <action android:name="fm.ex.android.setnowplaying" />
                <action android:name="fm.ex.android.start" />
             </intent-filter>
        </service>


## Extras

## MediaButtonHandler

To add support for handling media button clicks (the button on your headphones) to do play/pause/next/previous, add this receiver to your AndroidManifest.xml.

    <receiver android:name=".IEXAUdioMediaButtonHandler">
        <intent-filter android:priority="33000">
            <action android:name="android.intent.action.MEDIA_BUTTON" />
        </intent-filter>
    </receiver>

You'll also probably want to change the namespaces of the intents
so they dont clash with other apps possibly running the plugin.

## Remote Control

`IEXAudio` has an extra function `nowPlaying` to update the lock screen playback controls on Ice Cream Sandwich and greater.