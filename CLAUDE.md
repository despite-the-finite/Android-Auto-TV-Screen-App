# Car Screen — notes for future sessions

Sideloaded Android Auto app that shows streaming web players on the car screen while parked.
Kotlin, Car App Library 1.7.0, Gradle 8.11.1, AGP 8.9.3, compileSdk 35, minSdk 26.

## Build

```
set JAVA_HOME=C:\Users\admin\tools\jdk\jdk-17
./gradlew --no-daemon assembleDebug      # -> app/build/outputs/apk/debug/app-debug.apk
```

- `local.properties` must use forward slashes (`sdk.dir=C:/Users/admin/tools/android-sdk`).
  Single backslashes are swallowed by the properties parser and AGP then fails with
  "The filename, directory name, or volume label syntax is incorrect".
- The JDK folder was renamed to drop the `+` from the Temurin version string.
- Desktop Head Unit: `<sdk>/extras/google/auto/desktop-head-unit.exe`, needs
  `adb forward tcp:5277 tcp:5277` and "Start head unit server" on the phone.

## Architecture gotchas

- `NavigationTemplate` is used only because it is the sole template that gives a `Surface` on
  Android Auto. The manifest therefore declares the NAVIGATION category plus the
  `androidx.car.app.ACCESS_SURFACE` and `androidx.car.app.NAVIGATION_TEMPLATES` permissions.
  Removing any of these silently loses the surface.
- `CarDisplay` creates a *private* `VirtualDisplay` (PRESENTATION | OWN_CONTENT_ONLY) on the
  host surface and shows `WebPresentation` on it. Android picks `TYPE_PRIVATE_PRESENTATION`
  for the window automatically because the display is private; no overlay permission needed.
- Host touch events arrive as `SurfaceCallback.onClick/onScroll/onFling` (Car API level 2+).
  `onClick` is replayed as a synthetic DOWN/UP pair on the presentation's decor view.
- `StreamingChromeClient.onPermissionRequest` must grant `RESOURCE_PROTECTED_MEDIA_ID`, or
  Widevine (EME) never initializes and every DRM site shows a playback error.
- Both WebViews share one `CookieManager`, which is how phone-side login carries to the car.
- `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` is intentional: the app is never Play-distributed.
- Speed-based pause is best effort: Car API level 3+, needs the
  `com.google.android.gms.permission.CAR_SPEED` runtime permission; requested once, failure
  is logged and ignored.

## Unverified

Nothing has run on a phone or the DHU yet. Netflix on a virtual display depends on Widevine
behavior and is the first thing to test; use YouTube to separate DRM failures from projection
bugs.
