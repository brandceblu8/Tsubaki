# Project Tsubaki (椿)

A modern, fast, and accurate QR code scanner for Android, built with Kotlin and Jetpack Compose.

一个使用 Kotlin 和 Jetpack Compose 构建的现代化、快速且精准的安卓二维码扫描器。

---

## 📖 About the Project | 关于项目

Project Tsubaki was born from a simple goal: to create a QR code scanner that is as **accurate, fast, and minimalist** as the best commercial applications, but remains free and open. Tsubaki aims to provide a seamless scanning experience, focusing on core functionality and an intuitive user interface.

“椿”项目的诞生源于一个简单的目标：创造一个在**精准度、速度和简洁性**上都能媲美顶尖商业应用的二维码扫描器，同时保持其免费和开放的特性。“椿”致力于提供无缝的扫描体验，专注于核心功能和直观的用户界面。

This project also represents a personal learning journey into modern Android development with Kotlin. While I am still learning, I have leveraged AI assistance extensively to solve complex problems and accelerate the development process. It's a testament to how modern tools can empower developers to bring ambitious ideas to life.

这个项目也代表了我在使用 Kotlin 进行现代化安卓开发方面的一次个人学习之旅。在学习的过程中，我广泛地借助了 AI 的帮助来解决复杂问题并加速开发进程。它证明了现代工具如何能够赋能开发者，将宏大的想法变为现实。

## ✨ Key Features | 主要特性

* **⚡ Fast & Accurate Scanning**: Utilizes Google's ML Kit for high-performance barcode detection. (Support for ZXing is planned).
    > **⚡ 快速精准的扫描**：默认使用 Google ML Kit 实现高性能条码检测（计划支持 ZXing 引擎切换）。

* **📱 Modern UI**: A clean and simple interface built entirely with Jetpack Compose.
    > **📱 现代化的UI**：完全使用 Jetpack Compose 构建的简洁、美观的用户界面。

* **👆 Intuitive Controls**: A straightforward, "point-and-scan" user experience.
    > **👆 直观的操作**：简单易用的“即开即用”扫描体验。

* **🎨 Feature-Rich (Planned)**: Upcoming features include scan history, QR code creation, and cloud synchronization.
    > **🎨 功能丰富 (规划中)**：即将推出的功能包括扫描历史、二维码创建和云同步。

## 🛠️ Technology Stack | 技术栈

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Camera**: [CameraX](https://developer.android.com/training/camerax)
* **Scanning Engines**: [Google ML Kit](https://developers.google.com/ml-kit/vision/barcode-scanning), [ZXing](https://github.com/zxing/zxing)
* **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
* **Architecture**: MVVM (Model-View-ViewModel)

## 🚀 Getting Started | 如何开始

To build and run this project, you'll need:
1.  Android Studio (latest version recommended)
2.  Clone the repository:
    ```sh
    git clone https://github.com/NanCunChild/Tsubaki.git
    ```
3.  Open the project in Android Studio and let Gradle sync the dependencies.
4.  Run on an Android device or emulator.
