# DataWedge 設定步驟（給 DWBridge 專案）

以下使用繁體中文（Traditional Chinese）說明如何在 Zebra / TC27 等支援 DataWedge 的裝置上建立與本專案相容的 DataWedge Profile、如何把掃碼資料以 Intent 傳給 `com.nx.dwbridge`，以及如何在應用內停用鍵盤輸入、在 UI 上顯示掃描紀錄、以及如何透過 WebSocket 用 JavaScript 接收掃描事件。

目的
- 讓裝置掃描器把條碼透過 Intent 傳到本 APP（Action: `com.nx.dw.ACTION`、Category: `com.nx.dw.category.DEFAULT`）。
- 關閉「鍵盤輸入（Keystroke Output）」以免影響 UI 的輸入欄位。
- 讓 App 把接到的掃描資料轉成 JSON 並透過 WebSocket 推送給已連線的 clients（例如瀏覽器上的 JS）。

目標設定（請參考）
- Intent action：`com.nx.dw.ACTION`
- Intent category：`com.nx.dw.category.DEFAULT`
- Intent delivery method：Broadcast Intent（較推薦）
- Intent package（可選）：`com.nx.dwbridge`（若希望只發給本 app，請填）

步驟一：建立 DataWedge Profile
1. 在裝置上打開 DataWedge（或在 MDM / StageNow 裡建立 profile）。
2. 新增一個 Profile，建議命名：`DWBridge` 或 `DWBridge_Profile`。
3. 在 Profile 的 "Associated apps"（關聯應用程式）裡加入：
   - Application：選擇或輸入你的應用程式包名 `com.nx.dwbridge`。
   - 確保 Android target 對應的應用已選中。

步驟二：設定 Barcode Input（掃描器）
1. 在 Profile 下找到 "Barcode input" 或 "Input" 設定。
2. 啟用 scanner（例如 INTERNAL_IMAGER 或 INTERNAL_SCANNER）。
3. 檢查 Decoder（symbologies），開啟你需要的條碼類型（UPC-A, EAN13, QR, Code128...）。
4. 對於 TC27 可選擇觸發方式（continuous / single）視需求設定。

步驟三：關閉 Keystroke Output、啟用 Intent Output
1. 在 Profile 下找到 "Keystroke output"（或 "Keyboard output"）：**停用**（Disabled）。
   - 這會避免掃描結果直接作為鍵盤輸入到當前焦點欄位。
2. 在同一 Profile 裡找到 "Intent output"：**啟用**（Enabled）。
   - Delivery method（傳送方式）：選擇 `Broadcast intent`（或 `Start activity` 但 Broadcast 較通用）。
   - Intent action：填 `com.nx.dw.ACTION`。
   - Intent category：填 `com.nx.dw.category.DEFAULT`。
   - Intent package（可選）：填 `com.nx.dwbridge` 以限制只有你的 App 收到（若留空則發給所有有對應 Intent filter 的 App）。
   - Include data in intent extras：通常預設會包含 `com.symbol.datawedge.data_string` 或 `com.motorolasolutions.emdk.datawedge.data_string`，確定 extras 有被包含（通常 DataWedge 會自動包含）。

步驟四：（選擇）設定 Intent extras 的格式
- DataWedge 通常會把掃描結果放在 extras key：
  - `com.symbol.datawedge.data_string` 或 `com.motorolasolutions.emdk.datawedge.data_string`
  - `com.symbol.datawedge.label_type`（symbology）
  - `com.symbol.datawedge.scanner_identifier`（例如 INTERNAL_IMAGER）
- 若需要，DataWedge profile 可以設定要包含那些 extras，請確認已勾選「包含 decode_data / label_type / scanner identifier」等欄位。

步驟五：儲存並套用 Profile
1. 儲存 Profile，切換回 Home。
2. 在 DataWedge 主頁確認該 Profile 已啟用（Enabled）並與 `com.nx.dwbridge` 關聯。

App 端注意事項（已在 DWBridge 裡實作）
- `DataWedgeReceiver` 需要在 `AndroidManifest.xml` 或程式動態註冊時，註冊對 `com.nx.dw.ACTION` 的 broadcast receiver。
  - 例如 manifest 中：
    <receiver android:name=".dw.DataWedgeReceiver">
      <intent-filter>
        <action android:name="com.nx.dw.ACTION" />
        <category android:name="com.nx.dw.category.DEFAULT" />
      </intent-filter>
    </receiver>
- App 會在收到 Intent 後解析 extras（如 data_string、label_type），並將 JSON 轉發給內部的 WebSocket server（這在你的 logcat 中有看到：
  > Started WebSocket server on port 12345
  > DataWedgeReceiver received intent: action=com.nx.dw.ACTION ...
  > Started WebSocketService with scan JSON: {...}
  ）

要在 UI 停用輸入（當你按下 Start / Toggle 開始掃描時）
- 若你不希望掃描結果插入到目前 focus 的 EditText，可以：
  - 1) 在 DataWedge 關閉 Keystroke Output（最徹底）；或
  - 2) 在應用程式啟動 WebSocket 或開始接受掃描時，把 UI 的 EditText 設為不可編輯或 clear focus：
    - editText.setFocusable(false);
    - editText.setFocusableInTouchMode(false);
    - 或使用 editText.setEnabled(false);
  - 在停止時還原為 true。
- 另外，若你想要把掃描紀錄顯示在 UI（Log）上：
  - 當 `DataWedgeReceiver` 收到掃描 Intent 時，把解析後的掃描 JSON push 到一個 LiveData / Flow 或使用 runOnUiThread 更新 RecyclerView / TextView 的內容。

範例：掃描 Intent 可能包含的 extras（從你的 log）
- key: com.symbol.datawedge.data_string -> value: "628371193000"
- key: com.symbol.datawedge.label_type -> value: "LABEL-TYPE-UPCA"
- key: com.symbol.datawedge.scanner_identifier -> value: "INTERNAL_IMAGER"
- 你可以把它轉成如下 JSON 並交給 WebSocket 或 UI：
  {"type":"scan", "data":"628371193000", "symbology":"LABEL-TYPE-UPCA", "timestamp":1770827085431}

如何用 JavaScript (瀏覽器) 接收 WebSocket 推送的掃描事件
- 假設你的裝置 IP 為 `192.168.0.100`，App 內的 WebSocket server 監聽在 `12345` port。
- 瀏覽器（或 Node）可用下列 JS client：

```javascript
// Replace ip and port with your device's
const ws = new WebSocket('ws://192.168.0.100:12345');
ws.onopen = () => console.log('connected');
ws.onmessage = (evt) => {
  try {
    const msg = JSON.parse(evt.data);
    if(msg.type === 'scan'){
      console.log('Scan data:', msg.data, 'symbology:', msg.symbology);
      // update UI, send to server, etc.
    } else {
      console.log('Other message', msg);
    }
  } catch(e){
    console.log('Non-json message', evt.data);
  }
};
ws.onclose = () => console.log('closed');
ws.onerror = (err) => console.error('ws error', err);
```

注意：若使用 Chrome 手機版作為 client，要確保 WebSocket 可以從網路上連回裝置（同一 WiFi、或 port forwarding / reverse proxy）。

常見問題與排查
- 裝置沒收到 Intent：
  - 確認 DataWedge Profile 關聯的 App package 是否為 `com.nx.dwbridge`。
  - 確認 Intent action 與 category 與 App 的 receiver filter 完全一致。
  - 在 DataWedge 頁面測試掃描（有些 DataWedge 有 Test 按鈕可模擬 Intent 送出）。
- Logcat 沒看到任何訊息：
  - 確認使用 `adb logcat` 並檢查 `DWBridge` 或 `DataWedgeReceiver` 的 TAG。
  - 若使用 WiFi pairing，確認裝置與開發機之間的 logcat 連線仍然活躍。
- 為何掃描後還是會輸入到 EditText？
  - 檢查是否真的關閉了 Keystroke Output；或 App 在收到 Intent 時沒有 consume 或清掉焦點。
- Foreground Service 問題（針對 Android 13+/targetSdk 33+、或 35）：
  - 若 App 啟動 foreground service（例如 `WebSocketService`），並會呼叫 startForeground()，在 Android 12/13+ 若 targetSdk 高，必須在 `AndroidManifest.xml` 的 `<service>` 加上 `android:foregroundServiceType` 屬性，例如：
    - `android:foregroundServiceType="connectedDevice"`（或依需求，如 `location`, `mediaPlayback` 等）
  - 若你看到錯誤 `MissingForegroundServiceTypeException` 或 `SecurityException`，請依錯誤訊息把對應的 service type 與必要權限加入 manifest 或調整 targetSdk / 權限設定。

範例：在 `AndroidManifest.xml` 中註冊 receiver（確保跟 DataWedge action 一致）

```xml
<!-- ...existing manifest... -->
<application>
  ...
  <receiver android:name=".dw.DataWedgeReceiver" android:exported="true">
    <intent-filter>
      <action android:name="com.nx.dw.ACTION" />
      <category android:name="com.nx.dw.category.DEFAULT" />
    </intent-filter>
  </receiver>
  ...
</application>
```

補充：若你想要快速驗證 DataWedge 是否有發 Intent（不改 App）
- 在裝置上安裝一個第三方的 Intent 裝截工具（例如 Intent Sniffer），或使用 Termux + 小程式註冊 receiver，確認 DataWedge 是否發出你期望的 action/extra。

下一步（如果你要我代勞）
- 我可以幫你：
  1) 將應用 `Start` / `Stop` 按鈕改為 `On/Off` toggle 並在 UI 顯示掃描 log（RecyclerView 或 TextView）。
  2) 在 `DataWedgeReceiver` 收到掃描時同時更新 UI（LiveData/Flow）。
  3) 若需要，我也可以把 `AndroidManifest.xml` 補上適當的 `foregroundServiceType` 與說明，並協助修正 `MissingForegroundServiceTypeException`（你 log 裡曾出現的錯誤）。

如果你想要我直接修改專案檔案來：
- a) 新增 UI 的掃描 log 頁面，或
- b) 把 Start/Stop 換成 On/Off，並自動在 On 時停用 EditText、On/Off 的狀態儲存，
請回覆你要我先做哪一項，我會一步一步幫你修改並執行 build & 測試。

---
感謝！請告訴我接下來要我代做哪一件（例如：「把 start/stop 改成 toggle 並顯示掃描 log」）。
