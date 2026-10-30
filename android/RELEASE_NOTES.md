Standalone Android preview of Nathan 1.6.0, based on upstream commit
18313c07b6ab65fb3720fad6c890ad71b66ff86a.

Download **Nathan-Android.apk** and allow installation from your browser/file manager.
Android 8 or later, ARM64 phones/tablets and x86-64 emulators. No desktop server,
Termux, or separate Python installation is required. Internet is required for AI
providers, globe assets, and public cameras.

The APK embeds Nathan's existing Python engine, loopback API, and web UI. Provider
keys are encrypted with Android Keystore. Add your own free provider keys under
Accounts; nothing from the developer's desktop accounts is included.

Android-specific changes:

- On-device Python and WebView replace the desktop launcher and Edge window.
- Public camera lookup uses a hidden Android WebView, with results and playback
  shown inside Nathan. Individual feeds remain subject to provider restrictions.
- Core HTTP requests use standard HTTPX because the desktop HTTPX2 binary isn't
  available for Android. Python dependency versions are pinned for this build.
- Desktop games/apps, subscription CLI managers, subprocess Python tools, self-code
  upgrades and OS scheduled tasks aren't available on Android. API workers and
  their normal fallback paths run on the phone.
- Processes may be stopped by Android while backgrounded. This preview does not
  claim persistent background task execution or Play Store certification.

The build runs an Android emulator smoke test for engine startup, the local API
boundary, and encrypted key storage. Physical phone and every provider/feed still
need real-world testing. Release builds use a persistent signing key for updates.
