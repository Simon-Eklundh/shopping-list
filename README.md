# Shopping List

A minimal Google Keep–style shopping list app for Android.

- Add items with autocomplete suggestions based on everything you've added before
- No duplicates: Re-adding an item that's been checked off un-checks it again
- Checked ("bought") items collapse into a hidden **N checked items** section, like Keep
- Everything is stored locally on the device (SharedPreferences) — no network, no account

<img src="https://github.com/user-attachments/assets/95d65e94-4d72-46b4-8ee2-6e6a5bfda764" width="250" alt="Shopping list screen" />
<img src="https://github.com/user-attachments/assets/50ffcb0f-3564-4416-bad6-3b6991ef95ed" width="250" alt="Item detail screen" />

Requires **Android 16 (API 36)** or newer.

## Building the APK

### Prerequisites

- **Android SDK** with platform 36. The easiest way to get it is to install
  [Android Studio](https://developer.android.com/studio); the SDK then usually lives in
  `~/Android/Sdk` (Linux), `~/Library/Android/sdk` (macOS), or
  `%LOCALAPPDATA%\Android\Sdk` (Windows).
- **Java** on your `PATH` to launch Gradle. The build itself uses a JDK 21 toolchain that
  Gradle downloads automatically on first run, so the exact local version doesn't matter much.

Point the build at your SDK either by creating `local.properties` in the project root:

```properties
sdk.dir=/home/you/Android/Sdk
```

or by setting the `ANDROID_HOME` environment variable to the SDK path.
(Opening the project in Android Studio does this for you.)

### Build

From the project root:

```bash
./gradlew assembleDebug        # gradlew.bat assembleDebug on Windows
```

The APK ends up at:

```
app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is signed with an automatically generated debug key, which is perfectly fine
for sideloading onto your own device.

## Installing (sideloading)

### With adb (USB)

1. On the phone, enable **Developer options** (tap *Settings → About phone → Build number*
   seven times), then turn on **USB debugging**.
2. Connect the phone over USB and accept the debugging prompt.
3. Run:

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   (`-r` keeps the app's data when reinstalling over an existing install.)

### Without a computer

Copy `app-debug.apk` onto the phone (cloud drive, USB file transfer, etc.), then open it
with the Files app. Android will ask you to allow installs from that app the first time —
confirm, then install.

## Updating

Just build and install again with `adb install -r`. Your list and autocomplete history are
kept, as long as the APK is signed with the same key (which is automatic when building on
the same machine). If Android refuses the update with a signature error, uninstall the old
version first — note that this deletes the saved list.
