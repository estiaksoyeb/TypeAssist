# TypeAssist

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" />
</p>

<p align="center">
  <b>AI & Utility in every Android text field.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue.svg" />
  <img src="https://img.shields.io/badge/Kotlin-2.4.10-purple.svg" />
  <a href="https://github.com/estiaksoyeb/TypeAssist/releases"><img src="https://img.shields.io/github/downloads/estiaksoyeb/TypeAssist/total?logo=Github"/></a>
</p>

<p align="center">
  <a href="https://f-droid.org/en/packages/com.typeassist.app/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80" /></a>
</p>

TypeAssist uses Accessibility to trigger AI actions via simple commands, with support for multiple AI providers.

---

## 📸 Screenshots

  <!-- Screenshots -->
  <div align="center">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" width="30%"  alt=""/>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02.png" width="30%"  alt=""/>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03.png" width="30%"  alt=""/>
  </div>

---

## ✨ Features

### 🤖 AI Capabilities
*   **Ask AI:** Query Google Gemini, Cloudflare Workers AI, or **any OpenAI-compatible Custom API** directly from any app.
*   **Grammar Fix:** Instantly correct spelling, punctuation, and grammar errors.
*   **Translation:** Translate text from any language to English (or your preferred language).
*   **Tone Adjustment:** Rewrite messages to be more professional, polite, or friendly.
*   **Inline Commands:** Embed AI queries within sentences using `(.ta: your prompt)`.
*   **Global Rewrite:** Transform the entire text field with a custom instruction using `...instruction...`.
    *   Example: `meeting at 3pm, bring laptop ...expand to formal invite...`
    *   **Result:** "Please join us for a meeting at 3:00 PM. Kindly remember to bring your laptop as we will be working through some examples together."

### 🛠 Utility Belt (Offline Tools)
*   **Smart Calculator:** Solve math expressions in-place.
    *   Example: `(.c: 25 * 4 + 10)` -> `110`
*   **Snippets (Text Expander):** Expand shortcuts into full text blocks.
    *   Example: `..email` -> `user@example.com`
    *   **Quick Save:** Save new snippets instantly without opening the app: `(.save:trigger:content)`
*   **Date & Time:** Insert the current date or time using `.date` or `.now`. Both formats are customizable in Settings.
*   **Password Generator:** Generate a random 12-character password on the fly with `.pass`.

### 💾 Data Management
*   **Backup & Restore:** Securely export your settings, snippets, and API configurations to a `.tabak` file.
*   **Saved Configurations:** Save and switch between multiple API setups (e.g., "Personal Gemini", "Work Custom API").

### 🛡 Safety & Privacy
*   **Global Undo:** Revert any action instantly using `.undo`.
*   **History Manager:** View and recover original text from the last 5 minutes.
*   **Privacy First:** Sends text only when you use a trigger, and only to the provider you configured. History is kept in memory for 5 minutes and is not stored persistently.

---

## 📖 Usage Guide

### Standard Triggers
Type your text followed by a trigger to process it.

| Trigger | Action | Example |
| :--- | :--- | :--- |
| `.ta` | Ask AI | `Population of Tokyo? .ta` |
| `.g` | Fix Grammar | `i go home yestarday .g` |
| `.tr` | Translate | `你好世界 .tr` |
| `.polite` | Polite Tone | `Give me the money .polite` |
| `.casual` | Casual Tone | `We require your presence .casual` |
| `.improve` | Improve Writing | `the report needs done .improve` |
| `...` | Global Rewrite | `I am late ...make polite...` |
| `.undo` | Undo | Reverts the last replacement |
| `.date` | Insert Date | `Sent .date` |
| `.now` | Insert Date & Time | `Logged .now` |
| `.pass` | Random Password | `Password: .pass` |
| `(.c: ...)` | Calculator | `Total: (.c: 25 * 4 + 10)` |
| `..name` | Expand Snippet | `..email` |

A text trigger must appear at the end of the field by default. Inline commands such as `(.ta: ...)`, `(.g: ...)` and `(.polite: ...)` work anywhere inside a sentence, and Settings → Triggers & History lets you change the triggers, the snippet prefix (`..` by default) and whether triggers are allowed anywhere in the text.



---

## 🧪 Preview Builds

Want to try the latest features before they are officially released? You can download the latest automated builds here:

[![Download Preview Builds](https://img.shields.io/badge/Download-Preview%20Builds-red?style=for-the-badge)](https://estiaksoyeb.github.io/TypeAssist/)

---

## 📥 Installation & Setup

1.  **Download:** Install from [F-Droid](https://f-droid.org/en/packages/com.typeassist.app/) or the latest APK from the [Releases](https://github.com/estiaksoyeb/TypeAssist/releases) page.
2.  **Permissions:** Enable the **TypeAssist Accessibility Service** in your Android Settings.
3.  **API Key:** Open the app, go to **Settings**, and add your API keys.
    *   Supports Google Gemini, Cloudflare Workers AI, and Custom OpenAI Endpoints.
4.  **Start Typing:** Open any app (WhatsApp, Notes, Chrome) and try a trigger!

---

## 🤝 Support Development

If TypeAssist helps you in your daily workflow, consider supporting the development! Since traditional payment methods like PayPal are unavailable in my region, I accept donations via Binance and Cryptocurrency.

**Preferred Method (Zero Fees):**
*   **Binance Pay ID:** `724197813`

**Other Cryptocurrencies:**
*   **USDT (TRC20):** `TPP5S7HdV4Hrrtp5Cjz7TNtttUAfZXJz5a`
*   **TRX (Tron):** `TPP5S7HdV4Hrrtp5Cjz7TNtttUAfZXJz5a`

*Every bit helps keep this project open-source and covers the maintenance costs.*

---

## 🛠 Tech Stack
*   **UI:** Jetpack Compose (Material 3)
*   **Language:** Kotlin 2.4.10
*   **Network:** OkHttp / Retrofit / Gson
*   **Service:** Android AccessibilityService
*   **Architecture:** Single-activity Compose app with repository-based data layer

---

## 📜 License
Distributed under the **GPLv3 License**. See `LICENSE` for more information.

---

## 🔒 Privacy & Data Security

TypeAssist is designed with a **Zero-Middleman Architecture** to ensure your privacy is paramount.

### How We Handle Data
*   **Direct Connection:** Text is sent **directly** from your device to the provider you configure (Google Gemini, Cloudflare Workers AI, or any OpenAI-compatible endpoint, including a local model). **No intermediate servers** are used, meaning we have no access to your data.
*   **On-Demand Only:** The Accessibility Service runs locally. Text leaves your device only when you type a trigger that calls an online provider (e.g., `.ta`, `.g`).
*   **Open Source:** The entire codebase is open for audit. You can verify exactly how your data is handled.

### 📜 Provider Policies
Your input data is processed by the provider you configure. Please review their policies:
*   **Google Gemini:** [API Terms of Service](https://ai.google.dev/gemini-api/terms)
*   **Cloudflare Workers AI:** [Data Usage & Privacy](https://developers.cloudflare.com/workers-ai/platform/data-usage/)
