# TypeAssist

TypeAssist is a powerful Android Accessibility Service that acts as an intelligent keyboard assistant. It integrates AI (Google Gemini, Cloudflare Workers AI) and offline utility tools directly into any text input field on your device. Whether you need grammar correction, translation, quick calculations, or text snippets, TypeAssist is just a trigger away.

## Features

### 🤖 AI Capabilities
*   **Ask AI:** Query Google Gemini or Cloudflare Workers AI directly from any app.
*   **Grammar Fix:** Instantly correct spelling and grammar errors.
*   **Translation:** Translate text to English (default) or other languages.
*   **Tone Adjustment:** Rewrite messages to be more polite or professional.
*   **Inline Commands:** Embed AI queries within sentences using `(.ta: your prompt)`.

### 🛠 Utility Belt (Offline Tools)
*   **Smart Calculator:** Solve complex math expressions in-place.
    *   Supports `+`, `-`, `*`, `/`, `^`, `()`, `sqrt`, `sin`, `cos`, `tan`, `log`.
    *   Example: `(.c: 25 * 4 + 10)` -> `110`
*   **Snippets (Text Expander):** Expand shortcuts into full text blocks.
    *   Example: `ta#email` -> `user@example.com`
    *   Quick Save: `(.save:addr:123 Main St)`
*   **Date & Time:** Insert current timestamps.
    *   `.now` -> `2023-12-25 14:30`
    *   `.date` -> `Thursday, Dec 25`
*   **Password Generator:** Generate strong random passwords on the fly with `.pass`.

### 🛡 Safety & History
*   **Global Undo:** Revert any TypeAssist action using the `.undo` command or the dedicated UNDO button (active for 2 minutes).
*   **History Manager:** View and recover original text from the last 2 minutes via the History screen.
*   **Offline Mode:** Use snippets and utility tools without an internet connection or API key.

### ⚙️ Customization
*   **Multiple Providers:** Choose between Google Gemini (default) and Cloudflare Workers AI.
*   **Custom Triggers:** Create your own regex-based triggers for AI actions.
*   **Command Management:** Enable/disable specific commands via the UI.

## Usage Guide

### Standard Triggers
Type the text followed by the trigger to process it.

| Trigger | Action | Example |
| :--- | :--- | :--- |
| `.ta` | Ask AI | `Capital of France? .ta` |
| `.g` | Fix Grammar | `Im going home .g` |
| `.tr` | Translate | `Hola mundo .tr` |
| `.polite` | Polite Tone | `Send me the file .polite` |
| `.undo` | Undo Action | Reverts the last change |

### Inline Commands
Process specific parts of your text without affecting the rest.

*   **AI Query:** `I am visiting (.ta: capital of Japan) next week.`
*   **Calculator:** `The total is (.c: 50 * 1.2) dollars.`

### Snippets
*   **Expand:** Type `ta#` followed by your snippet name (e.g., `ta#mybio`).
*   **Create:** Use `(.save:name:content)` to create a snippet named "name" with "content".

## Installation

1.  **Download & Install** the APK.
2.  **Grant Permissions:**
    *   Open the app and follow the prompt to enable the **Accessibility Service** for TypeAssist.
    *   This is required to read and replace text in other apps.
3.  **Configure API Key:**
    *   Go to **Settings**.
    *   Select your AI Provider (Gemini or Cloudflare).
    *   Enter your API Key.
    *   (Optional) Enable "Offline Mode" to use only Utility Belt features.

## Development

### Tech Stack
*   **Language:** Kotlin
*   **UI:** Jetpack Compose (Material3)
*   **Architecture:** MVVM
*   **Service:** Android AccessibilityService
*   **Networking:** OkHttp, Retrofit/Gson
*   **Build System:** Gradle

### Project Structure
*   `app/src/main/kotlin/com/typeassist/app/service/MyAccessibilityService.kt`: Core logic for text monitoring and replacement.
*   `app/src/main/kotlin/com/typeassist/app/utils/UtilityBelt.kt`: Logic for offline tools (Math, Date, Password).
*   `app/src/main/kotlin/com/typeassist/app/ui`: Jetpack Compose UI screens.

## Privacy Note
TypeAssist uses an Accessibility Service to function. It processes text *only* when you type a specific trigger.
*   **No data is stored permanently** unless you save a Snippet.
*   **History is ephemeral** (cleared after 2 minutes or app restart).
*   **Internet access** is used solely for AI API calls (Gemini/Cloudflare) and checking for updates.

## Credits
*   **Icons:** Font Awesome 5 Free (Brand icons) & Material Icons.
*   **AI:** Google Gemini & Cloudflare Workers AI.
