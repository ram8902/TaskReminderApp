# Task Reminder App

A modern, smart Android application to manage your tasks, study schedules, and reminders effectively. The app is built with Kotlin, Room, WorkManager, and Jetpack Compose featuring a robust Android architecture.

## Features

- **Smart Reminders**: Schedule tasks with specific start and end dates.
- **Background Notifications**: Uses `WorkManager` and `AlarmManager` for reliable notifications even when the app is closed.
- **Customizable Alarms**: Set your preferred alarm sound and adjust the alarm volume independently of system settings.
- **Home Screen Widgets**: Instantly view and manage your tasks from your home screen with full functional dark mode support.
- **Modern UI**: Fully built with Jetpack Compose using Material 3 design guidelines.
- **Local Database**: All active and completed tasks are tracked securely on your device using a Room Database.

## Tech Stack

- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Local Data**: Room Database (`androidx.room`)
- **Background Processing**: WorkManager, AlarmManager
- **Asynchronous Data**: Kotlin Coroutines & `StateFlow`

## Architecture Overview

The app follows standard modern Android Architecture guidelines:
1. **UI Layer**: Composed of Jetpack Compose screens and ViewModels (e.g., `TaskViewModel`) that manage UI state. They act as the single source of truth for the screen.
2. **Data Layer**: Powered by Room (`AppDatabase`, `TaskDao`) for safe and structured access to persistent data.
3. **Background Services**: A specialized `ReminderManager` object interfaces directly with Android system services to orchestrate all scheduled work and system-level alarms.

## Setup & Installation

1. Clone or download this repository.
2. Open the project in Android Studio (Giraffe/Hedgehog or later recommended).
3. Let Gradle sync and download required dependencies.
4. Run the app on an Android emulator or a physical device running Android 8.0 (Min SDK API 26) or higher.

## Download Link
link: 
