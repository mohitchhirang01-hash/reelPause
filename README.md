# AppPause

Shows a 60-second countdown before any app you choose opens (Instagram, YouTube, anything). When the timer ends you can open the app or back out.

## Run it

1. Open this folder in **Android Studio** (Koala or newer) and let Gradle sync. If it asks to create or upgrade the Gradle wrapper, accept.
2. Plug in your phone (USB debugging on) and press Run, or use Build > Build APK(s) and install the APK.
3. In the app:
   - Tap **Turn on accessibility service**, find **AppPause**, switch it on.
   - Tap **Allow display over other apps** and allow it.
   - Tick the apps you want gated (or **Select all**).
4. Open one of those apps. The countdown appears first.

## How it works

- `GateService` is an accessibility service that only listens for "a new app came to the foreground". It does not read screen content.
- If that app is ticked and its grace period has expired, it launches `TimerActivity`.
- `TimerActivity` counts down. **Open** becomes enabled at 0 and starts a grace period (default 5 min) so you aren't blocked again straight away. **Never mind** or Back sends you home. Leaving the screen mid-countdown cancels the wait.
- `Prefs` stores the ticked apps, countdown length, and grace period.

## Settings

Countdown seconds and grace minutes are editable on the main screen. Set grace to 0 to wait every single time.

## Phone-specific gotchas

On Xiaomi, Oppo, Vivo, OnePlus and Samsung, the system may kill background services. If the countdown stops appearing, turn off battery optimization for AppPause ("Unrestricted" battery) and allow auto-start.

## Not included

- No app icon (Android shows a default one). Add one via New > Image Asset.
- Android only; iOS can't do this from a normal app.
