# 🛡️ Secure Browser - Android

A privacy-first Android browser built with Kotlin.

## Features
- 🛡️ Built-in Ad Blocker (100+ domains)
- 🦆 DuckDuckGo default search engine
- 🔒 HTTPS enforced, no mixed content
- 🚫 No third-party trackers / cookies
- 🌙 Dark Mode support
- 👁️ Incognito Mode (no history, no cookies)
- ⬇️ Download Manager
- 📱 Chrome-like Material Design UI
- 🔗 In-app link opening
- 📋 Multi-tab support

## How to Build
1. Open project in Android Studio (Hedgehog or newer)
2. Sync Gradle
3. Run on device/emulator (Android 7.0+)

## Project Structure
```
app/src/main/
├── java/com/securebrowser/
│   ├── MainActivity.kt      ← Main browser logic
│   ├── AdBlocker.kt         ← Ad/tracker blocking
│   ├── BrowserTab.kt        ← Tab data model
│   └── adapters/
│       └── TabAdapter.kt    ← Tab list adapter
└── res/
    ├── layout/              ← UI layouts
    ├── drawable/            ← Icons & shapes
    └── values/              ← Colors, strings, themes
```
