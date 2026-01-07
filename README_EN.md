# Luban

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/top.zibin/luban.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:top.zibin%20a:luban)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

[English](README_EN.md) | [中文](README.md)

`Luban` — An `Android` image compression tool, mimicking the compression strategy of WeChat Moments.

# 📖 Project Description

Image handling is an unavoidable element in `App` development. With the increasing resolution of mobile cameras, image compression has become a critical issue. While there are many articles on simple cropping and compression, controlling the exact parameters is difficult—cropping too much results in tiny images, while over-compressing leads to poor display quality.

Naturally, one wonders how the industry giant "WeChat" handles this. `Luban` was developed by reverse-engineering the compression algorithm used by WeChat Moments, achieved by sending nearly 100 images of varying resolutions and comparing the originals with the compressed versions.

Because it is a reverse-engineered estimation, the results cannot be exactly identical to WeChat's, but they are very close. See the comparison below!

This library is the **Kotlin refactored version** of `Luban`. While upgrading the core algorithm, it has been deeply optimized using **Kotlin Coroutines** and **TurboJPEG**. The new algorithm is more robust and efficient than the original, providing more efficient asynchronous processing and superior compression quality.

# 📊 Effects & Comparison

| Image Type | Original | Luban | Wechat |
| :--- | :--- | :--- | :--- |
| **Standard Photo** | 3024*4032, 5.10MB | 1440*1920, 305KB | 1440*1920, 303KB |
| **High-Res Photo** | 4000*6000, 12.10MB | 1440*2160, 318KB | 1440*2160, 305KB |
| **2K Screenshot** | 1440*3200, 2.10MB | 1440*3200, 148KB | 1440*3200, 256KB |
| **Long Screenshot** | 1242*22080, 6.10MB | 758*13490, 290KB | 744*13129, 256KB |
| **Panorama** | 12000*5000, 8.10MB | 1440*600, 126KB | 1440*600, 123KB |
| **Design Draft** | 6000*6000, 6.90MB | 1440*1440, 263KB | 1440*1440, 279KB |

## 🔬 Core Algorithm Features

This library uses an **Adaptive Unified Image Compression** algorithm that dynamically applies differentiated strategies based on the original image's resolution characteristics to achieve optimal balance between quality and file size.

### Intelligent Resolution Decision

- **High-Definition Baseline (1440p)**: Uses 1440px as the default short-side baseline, ensuring visual clarity on modern 2K/4K displays
- **Panorama Wall Strategy**: Automatically identifies ultra-wide panoramas (long side >10800px), locks the long side to 1440px while preserving the full field of view
- **Mega-Pixel Trap**: Automatically applies 1/4 downsampling to images exceeding 41MP (4096万 pixels)
- **Long Image Memory Protection**: Establishes a 10.24MP pixel cap for ultra-long screenshots, preventing OOM through proportional scaling

### Adaptive Bitrate Control

- **Tiny Images (<0.5MP)**: Minimal lossy compression to prevent compression artifacts
- **High-Frequency Images (0.5-1MP)**: Enhanced encoding quality to compensate for resolution loss
- **Standard Images (1-3MP)**: Balanced coefficients matching mainstream social media apps
- **Large/Long Images (>3MP)**: High compression ratios to significantly reduce file size

### Robustness Guarantees

- **Inflation Fallback**: Automatically returns the original image if compressed size exceeds original, ensuring no "negative optimization"
- **Smart Format Passthrough**: Preserves transparency for small PNG files, auto-converts large PNG files to JPEG
- **Input Defense**: Safely handles extreme resolution inputs (0, negative, 1px, etc.), preventing crashes

# 📦 Import

Add the dependency to your module's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("top.zibin:luban:2.0.0")
}
```

# 💻 Usage

### ⚡ Kotlin (Coroutines)

The most idiomatic way to use the library in Kotlin is via `suspend` functions.

#### Compress a Single File

```kotlin
lifecycleScope.launch {
    val inputUri: Uri = ... // Your image Uri
    val outputDir = context.cacheDir
    
    Luban.compress(context, inputUri, outputDir)
        .onSuccess { file ->
            // Compression successful, 'file' is the compressed image
            Log.d("Luban", "Compressed: ${file.absolutePath}")
        }
        .onFailure { error ->
            // Handle error
            Log.e("Luban", "Error: ${error.message}")
        }
}
```

#### Compress Multiple Files Concurrently

```kotlin
lifecycleScope.launch {
    val uris: List<Uri> = ... 
    val outputDir = context.cacheDir

    // Returns a List<Result<File>>
    val results = Luban.compress(context, uris, outputDir)
    
    results.forEach { result ->
        result.onSuccess { file -> 
            // ...
        }
    }
}
```

### ☕ Java / Builder Pattern

For Java projects or if you prefer a callback-based approach, use the `Luban.with()` API which is compatible with the original library style.

#### Compress a Single File

```java
Luban.with(context)
    .load(imageFile) // Can be File, Uri, or String path
    .setTargetDir(context.getCacheDir())
    .bindLifecycle(lifecycleOwner) // Optional: Auto-cancel on destroy
    .setCompressListener(new OnCompressListener() {
        @Override
        public void onStart() {
            // Compression started
        }

        @Override
        public void onSuccess(File file) {
            // Compression finished successfully
        }

        @Override
        public void onError(Throwable e) {
            // An error occurred
        }
    })
    .launch();
```

#### Compress Multiple Files

```java
List<String> imagePaths = ...; // List of image paths

Luban.with(context)
    .load(imagePaths) // Load a list of images
    .setTargetDir(context.getCacheDir())
    .setCompressListener(new OnCompressListener() {
        @Override
        public void onStart() {
            // Compression started
        }

        @Override
        public void onSuccess(File file) {
            // Called for EACH successfully compressed image
            Log.d("Luban", "Compressed: " + file.getAbsolutePath());
        }

        @Override
        public void onError(Throwable e) {
            // An error occurred
        }
    })
    .launch();
```

# ☕ Donation

If this project has been helpful to you, please consider supporting my work through the following methods. Your support is the motivation for me to continue improving and maintaining this project.

<div align="center">

<table>
<tr>
<td align="center" style="padding-right: 20px;">
<img src="images/alipay.png" width="300" alt="Alipay QR Code" />
</td>
<td align="center" style="padding-left: 20px;">
<img src="images/wechat.png" width="300" alt="WeChat QR Code" />
</td>
</tr>
</table>

</div>

Thank you for your support! 🙏

# 📄 License

    Copyright 2025 Zibin
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

