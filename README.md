# Car Screen

Watch streaming sites on your car's Android Auto display while parked. The app runs an
embedded browser (desktop user agent, Widevine DRM enabled) on the car screen and gives you a
phone-side remote for logging in and typing.

> Personal-use, sideloaded project. Android Auto does not permit video apps on the Play Store,
> so this app is installed by hand and enabled through Android Auto's developer settings.
> Only use it while parked.

## How it works

- The app registers with Android Auto as a navigation app, because that is the only app
  category that is handed a drawing `Surface` on the car screen.
- On that surface it creates a private `VirtualDisplay` and shows a full-screen `Presentation`
  containing a `WebView`.
- Taps, scrolls and flings reported by Android Auto are replayed into the WebView.
- The phone app hosts a second WebView sharing the same cookie jar, so you log in on the phone
  and the car screen is already signed in. The phone also has buttons to send a page to the car,
  type into the focused field on the car page, press Enter, go back, reload, play and pause.
- If the host exposes vehicle speed (Car App API level 3+), playback is paused and a warning
  is shown whenever the car is moving.

Netflix's web player depends on Widevine DRM. Whether it plays on a virtual display depends on
the phone's Widevine level and Netflix's checks. YouTube, Plex, Jellyfin, Tubi and similar
sites work without that caveat.

## One-time phone setup

1. Enable Developer options: Settings > About phone > tap **Build number** seven times.
   Then Settings > System > Developer options > **USB debugging** on.
2. Enable Android Auto developer settings: Settings > Connected devices > Connection
   preferences > Android Auto. Scroll to the bottom and tap **Version** about ten times, accept
   the prompt, then open the three-dot menu > **Developer settings**.
3. In Developer settings turn on **Unknown sources**. Without this, Android Auto hides the app.

## Build

Requires JDK 17 and an Android SDK with platform 35 and build-tools 34.0.0. Put the SDK
location in `local.properties`:

```
sdk.dir=C:/path/to/android-sdk
```

```
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Install it with
`adb install -r app/build/outputs/apk/debug/app-debug.apk` or copy it to the phone and open it.

## Test at your desk with the Desktop Head Unit

The Desktop Head Unit (DHU) is Google's car-screen simulator. Install it with the SDK manager
package `extras;google;auto`; it lives at `<sdk>/extras/google/auto/desktop-head-unit.exe`.

1. Connect the phone over USB and accept the USB debugging prompt.
2. In Android Auto developer settings on the phone, tap **Start head unit server**. A
   persistent notification confirms it is running.
3. On the PC:

   ```
   adb forward tcp:5277 tcp:5277
   cd <sdk>/extras/google/auto
   desktop-head-unit.exe
   ```

4. A car screen window opens. Find **Car Screen** in its app launcher and open it.
5. On the phone, open the Car Screen app, log in to Netflix in the phone browser, then tap
   **Send this page to car**.

Mouse clicks and scroll in the DHU window behave like touches on a real head unit.

## In the car

Connect as usual, open Car Screen from the Android Auto launcher while parked, and pick a title.
Use the phone app to type into search fields on the car page.

## Layout

```
app/src/main/java/com/despitethefinite/carscreen/
  MainActivity.kt           phone UI: login browser + car remote
  ProjectionController.kt   bridge between phone UI and car display
  WebConfig.kt              shared WebView settings, DRM grant, fullscreen handling
  car/CarScreenService.kt   Android Auto entry point
  car/CarScreenSession.kt
  car/ProjectionScreen.kt   NavigationTemplate shell, action strip, speed monitor
  car/CarDisplay.kt         VirtualDisplay + input forwarding
  car/WebPresentation.kt    full-screen WebView on the virtual display
```
