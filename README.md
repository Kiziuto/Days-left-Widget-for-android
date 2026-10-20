# Life in Days

Home-screen widget that shows the **days left in your life** as a glowing, futuristic countdown.

- Add the widget -> a settings screen opens -> set your **date of birth** and **lifespan in years** -> Save.
- The widget redraws itself every midnight. Tap it any time to change the settings.
- The settings screen shows a live preview of the widget as you type.

## Get the APK onto your phone

### Option A: Android Studio (on a PC)
1. Open this folder in Android Studio (Ladybug or newer) and let Gradle sync.
2. Build > Build APK(s). The file is `app/build/outputs/apk/debug/app-debug.apk`.
3. Send it to your phone (cable, Drive, email) and open it.

### Option B: build in the cloud, download on your phone
1. Create a GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab, run **Build APK** (it also runs on every push).
3. When it finishes, open the repo's **Releases** page on your phone and download `LifeInDays.apk`.
   (The repo must be public, or you must be signed in to GitHub on the phone.)

### Install
Android asks to allow "Install unknown apps" for your browser or file manager the first time. Allow it, then open the APK.
Then: long-press the home screen > Widgets > **Life in Days**.

## Notes
- Requires Android 8.0 (API 26) or newer.
- The build is signed with the standard debug key, which is fine for personal use.
  To update later, install the new APK over the old one (same key).
- Futuristic numerals use the Orbitron typeface (SIL Open Font License, see `FONT-LICENSE-Orbitron-OFL.txt`).
