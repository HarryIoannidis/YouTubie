# YouTubie

YouTubie is a modern Android application designed to provide a seamless video discovery and playback experience. Built with the latest Android technologies, it leverages powerful libraries for networking, image loading, and dependency injection to ensure a fast and reliable user interface.

## Features

- **Video Search & Discovery**: Effortlessly search for and discover new content.
- **Modern UI**: Clean and intuitive user interface following Material Design guidelines.
- **Picture-in-Picture (PiP)**: Continue watching videos while using other apps.
- **Hilt Dependency Injection**: Robust and scalable architecture using Dagger Hilt.
- **Efficient Networking**: Fast API communication using Retrofit and OkHttp.
- **Image Caching**: Smooth image loading and caching powered by Glide.
- **Coroutines & Flow**: Asynchronous programming for a responsive user experience.

## Tech Stack

- **Kotlin**: The primary language for Android development.
- **Retrofit & OkHttp**: For networking and API interactions.
- **Glide**: For efficient image loading.
- **Dagger Hilt**: For dependency injection.
- **Jetpack Components**:
    - ViewModel & LiveData
    - View Binding
    - WorkManager
    - App Startup
- **Material Design 3**: Modern UI components.

## Getting Started

### Prerequisites

- Android Studio Koala or newer.
- Android SDK 37 (API level 37).
- A RapidAPI Key (for YouTube data).

### Configuration

Before building the app, you need to provide your `RAPID_API_KEY`. 

1. Create a `local.properties` file in the root directory if it doesn't exist.
2. Add your API key:
   ```properties
   RAPID_API_KEY=your_actual_api_key_here
   ```

### Building & Running

1. Clone the repository:
   ```bash
   git clone https://github.com/HarryIoannidis/YouTubie.git
   ```
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Run the app on an emulator or a physical device (API level 23+).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
