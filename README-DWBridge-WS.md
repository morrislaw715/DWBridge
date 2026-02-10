DWBridge — DataWedge → WebSocket bridge (Zebra TC27)

Overview

This project adds a lightweight WebSocket server to the DWBridge Android app. The service listens for DataWedge scan intents and forwards scanned payloads as JSON to all connected WebSocket clients on the LAN.

What I changed / added

- app/build.gradle.kts: added dependencies:
  - org.java-websocket:Java-WebSocket:1.5.3
  - com.google.code.gson:gson:2.10.1 (note: Gson was used originally but receiver now uses JSONObject; dependency kept in case you prefer Gson later)

- app/src/main/AndroidManifest.xml:
  - added uses-permission INTERNET and FOREGROUND_SERVICE
  - registered `com.nx.dwbridge.ws.WebSocketService` (foregroundServiceType="connectedDevice")
  - registered `com.nx.dwbridge.dw.DataWedgeReceiver` as a broadcast receiver

- New Kotlin files:
  - `app/src/main/java/com/nx/dwbridge/ws/WsMessage.kt` (data model)
  - `app/src/main/java/com/nx/dwbridge/ws/WebSocketService.kt` (foreground Service hosting Java-WebSocket server)
  - `app/src/main/java/com/nx/dwbridge/dw/DataWedgeReceiver.kt` (BroadcastReceiver that receives DataWedge intents and starts the service with JSON payload)
  - `app/src/main/java/com/nx/dwbridge/ui/ServerViewModel.kt` (ViewModel for UI state)
  - `app/src/main/java/com/nx/dwbridge/ui/home/MessagesAdapter.kt` (RecyclerView adapter)

- Layout changes:
  - `app/src/main/res/layout/fragment_home.xml` replaced to include port input, start/stop button, status line, and RecyclerView
  - `app/src/main/res/layout/item_message.xml` added for message rows

- gradle.properties: added commented guidance to set `org.gradle.java.home` to a Java 17 JDK on your machine.

Notes about building

- Android Gradle Plugin (AGP) requires Java 17; your environment currently has Java 11. On macOS please install a Java 17 JDK and either configure Android Studio to use it or set `org.gradle.java.home` in `gradle.properties`.

Example (macOS, using Temurin):

1. Install Temurin 17 (on your machine):

```bash
# If you have Homebrew:
brew install temurin17
# Or download from Eclipse Temurin:
# https://adoptium.net/
```

2. Point Gradle to Java 17 (one-time):

Open `gradle.properties` and add (example path):

```
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

3. Build the debug APK:

```bash
./gradlew assembleDebug
```

Running on Zebra TC27 and DataWedge setup

1. Configure DataWedge profile to send broadcasts:
   - Output: Intent
   - Intent action: com.nx.dwbridge.DATAWEDGE
   - Intent delivery: Broadcast Intent
   - Include the following extras: data_string and label_type

2. Install the app onto the device (after building):

```bash
./gradlew :app:installDebug
```

3. Start the WebSocket server in the app (Home tab) and note the port (default 8080).

Testing with a WebSocket client

- wscat (Node):

```bash
npm install -g wscat
wscat -c ws://<device-ip>:8080
```

- Browser JS console:

```js
const ws = new WebSocket('ws://<device-ip>:8080');
ws.onmessage = e => console.log('msg', e.data);
```

Send a fake DataWedge intent via adb to test the end-to-end flow:

```bash
adb shell am broadcast -a com.nx.dwbridge.DATAWEDGE --es com.symbol.datawedge.data_string "TEST123" --es com.symbol.datawedge.label_type "CODE128"
```

The app's HomeFragment RecyclerView should show the scan and connected WebSocket clients (logs available via `adb logcat -s DWBridge`).

Security and production notes

- The current implementation uses plaintext ws://. For production, terminate TLS (wss://) at a reverse proxy or implement SSL on the server.
- Consider an authentication handshake for clients.
- Consider handling network changes (restart server when Wi-Fi IP changes).

Next steps I can take for you

- Implement token-based handshake and client limit.
- Add persistence for port and auth token in SharedPreferences and expose in UI.
- Add unit tests for JSON creation and ViewModel behavior.
- If you want, I can apply a PR-style patch or create the missing files as a proper branch.

If you want me to continue, say what you'd like next (e.g., implement auth, implement WSS, add tests, or finish refinements to UI).
