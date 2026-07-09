# YouTubie Android App

![Kotlin](https://img.shields.io/badge/language-Kotlin-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)

Modern YouTube download experience for Android - built with Kotlin and Retrofit

YouTubie is a sleek video discovery prototype built to demonstrate a modern Android development stack. It provides a seamless interface for searching, and downloading video content.


## Features
*   **🔍 High-speed video search** - find content instantly via RapidAPI integration.
*   **📺 Seamless download** - integrated video downloader with Material Design controls.
*   **🖼️ Glide image caching** - smooth, flicker-free thumbnail loading and memory management.
*   **🏗️ Hilt Dependency Injection** - robust, testable, and scalable architecture.
*   **⚡ Coroutines & Flow** - modern asynchronous programming for a responsive UI.
*   **🎨 Material Design 3** - follows the latest Android design guidelines.
*   **⚙️ WorkManager integration** - reliable background task processing.

## Screenshots
*   Home Dashboard
*   Video Search Results

For a full walkthrough of all interface screens, workflows, and dialogs, see the `SCREENSHOTS.md` tour.

## Quick Start

### Prerequisites
Android Studio Koala+, Android SDK 37, RapidAPI Account.

### 1. Clone the repository
```bash
git clone https://github.com/HarryIoannidis/YouTubie.git
cd YouTubie
```

### 2. Configure API Keys
The app requires a `RAPID_API_KEY` to fetch data.
1. Create a `local.properties` file in the root directory.
2. Add your key:
```properties
RAPID_API_KEY=your_actual_rapidapi_key_here
```

### 3. Build the project
Open the project in Android Studio and sync with Gradle.

### 4. Run the app
```bash
./gradlew installDebug
```
Or use the **Run** button in Android Studio on a device with API level 23+.

## Architecture
```
youtubie/
├── app/
│   ├── src/main/java/com/youtubie/app/
│   │   ├── data/        # Repository and Remote Data Source (Retrofit)
│   │   ├── di/          # Hilt Modules (Network, Repository)
│   │   ├── ui/          # ViewModels, Fragments, and Activities
│   │   └── util/        # Preferences and UI helpers
│   └── src/main/res/    # Material 3 resources, layouts, and animations
```

### Technical Interface
| Component | Implementation |
| :--- | :--- |
| **Networking** | Retrofit 2 + OkHttp 4 |
| **Dependency Injection** | Dagger Hilt |
| **Image Loading** | Glide |
| **Concurrency** | Kotlin Coroutines + Flow |
| **UI Framework** | Jetpack (ViewBinding, LiveData, ViewModel) |
| **JSON Parsing** | GSON |
| **Background Tasks** | WorkManager |

## Security and Configuration
*   **API Key Protection**: The `RAPID_API_KEY` is injected via `BuildConfig` and sourced from `local.properties`, ensuring secrets are never committed to version control.
*   **Secure Networking**: Enforces HTTPS for all API calls via Retrofit configuration.
*   **ProGuard/R8**: Hardened release builds with optimized code shrinking and obfuscation.

## Contributing
See `CONTRIBUTING.md` for guidelines.

## License
MIT © YouTubie Contributors
