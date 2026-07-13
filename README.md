# YouTubie Android App

![Kotlin](https://img.shields.io/badge/language-Kotlin-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)

**YouTubie** is a high-performance video discovery and download prototype for Android. Built with a modern tech stack and a focus on speed, it demonstrates a modular architecture capable of handling multi-threaded data transfers and delivering a consistent, immersive user experience.

---

## 🚀 Key Pillars

### 1. High-Performance Speed
*   **Multi-threaded Engine**: Utilizes a custom 4-thread parallel download system with automatic chunk merging.
*   **Range-Based Fetching**: Implements HTTP Range headers to maximize bandwidth usage during audio and video downloads.
*   **Optimized Throughput**: Features a large 128KB data buffer for smooth, high-speed transfers on modern networks.

### 2. Modern Design System
*   **Unified UI**: A cohesive card-based design language shared between Search Results and Download History.
*   **Geometric Precision**: Carefully aligned curvatures and spacing for a polished, professional feel.
*   **Immersive Experience**: Full-screen splash phase and smooth activity-fade transitions for seamless navigation.

### 3. Robust Architecture
*   **Dagger Hilt DI**: Clean dependency injection for testability and scalability.
*   **WorkManager Integration**: Reliable background task handling for long-running parallel transfers.
*   **Coroutines & Flow**: Reactive UI state management for a flicker-free, responsive interface.

---

## 🛠️ Technical Deep Dive

### Parallel Download Engine
The core of YouTubie's performance lies in its `DownloadWorker`. Instead of standard sequential streams, the app:
1.  **Analyzes**: Checks server support for `Accept-Ranges`.
2.  **Segments**: Splits the target file into 4 equal segments.
3.  **Executes**: Spawns concurrent Coroutines to fetch segments in parallel.
4.  **Synchronizes**: Uses `FileChannel` to write segments to specific file offsets simultaneously, ensuring zero data corruption and maximum speed.

### Tech Stack
| Layer | Implementation |
| :--- | :--- |
| **Networking** | Retrofit 3 + OkHttp 5 |
| **DI** | Dagger Hilt |
| **Image Loading** | Glide 5 (with optimized caching) |
| **Concurrency** | Kotlin Coroutines + StateFlow |
| **Persistence** | SharedPreferences + GSON |
| **Background Tasks** | WorkManager |

---

## 🚦 Quick Start

### Prerequisites
*   Android Studio Koala or newer.
*   Android SDK 37.
*   A valid [RapidAPI](https://rapidapi.com/) account with the YouTube Data API enabled.

### 1. Configure API Keys
The app requires a `RAPID_API_KEY` to function.
1. Create a `local.properties` file in your root directory.
2. Add your key:
```properties
RAPID_API_KEY=your_actual_rapidapi_key_here
```

### 2. Build and Run
Open the project in Android Studio, sync with Gradle, and deploy to a device with API level 23+.
```bash
./gradlew installDebug
```

---

## 🏗️ Architecture Layout
```text
youtubie/
├── app/
│   ├── src/main/java/com/youtubie/app/
│   │   ├── data/        # Repository, API Services, and Parallel Download Engine
│   │   ├── di/          # Hilt Dependency Injection Modules
│   │   ├── ui/          # ViewModels, Fragments, and Polished Activities
│   │   └── util/        # Preference Management, Storage Utils, and UI Helpers
│   └── src/main/res/    # Material 3 resources, Layouts, and Immersive Animations
```

---

## 📄 License
MIT © YouTubie Contributors
