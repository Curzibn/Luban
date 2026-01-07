# Luban

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/top.zibin/luban.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:top.zibin%20a:luban)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

[中文](README.md) | [English](README_EN.md)

Luban 2（鲁班 2） —— 高效简洁的 Android 图片压缩工具库，像素级还原微信朋友圈压缩策略。

## 📑 目录

- [📖 项目描述](#-项目描述)
- [📊 效果与对比](#-效果与对比)
  - [🔬 核心算法特性](#-核心算法特性)
- [📦 导入](#-导入)
- [💻 使用](#-使用)
  - [⚡ Kotlin (Coroutines)](#-kotlin-coroutines)
  - [☕ Java / Builder 模式](#-java--builder-模式)
- [☕ 捐助](#-捐助)
- [📄 License](#-license)

# 📖 项目描述

目前做`App`开发总绕不开图片这个元素。但是随着手机拍照分辨率的提升，图片的压缩成为一个很重要的问题。单纯对图片进行裁切，压缩已经有很多文章介绍。但是裁切成多少，压缩成多少却很难控制好，裁切过头图片太小，质量压缩过头则显示效果太差。

于是自然想到`App`巨头"微信"会是怎么处理，`Luban`（鲁班）就是通过在微信朋友圈发送近100张不同分辨率图片，对比原图与微信压缩后的图片逆向推算出来的压缩算法。

因为是逆向推算，效果还没法跟微信一模一样，但是已经很接近微信朋友圈压缩后的效果，具体看以下对比！

本库是 `Luban` 的 **Kotlin 重构版本**，在升级核心算法的同时，利用 **Kotlin Coroutines** 和 **TurboJPEG** 进行了深度优化。新算法比原算法更加健壮和高效，提供更高效的异步处理和更优质的压缩效果。

# 📊 效果与对比

| 图片类型 | 原图（分辨率, 大小） | Luban（分辨率, 大小） | Wechat（分辨率, 大小） |
| :--- | :--- | :--- | :--- |
| **标准拍照** | 3024×4032, 5.10MB | 1440×1920, 305KB | 1440×1920, 303KB |
| **高清大图** | 4000×6000, 12.10MB | 1440×2160, 318KB | 1440×2160, 305KB |
| **2K 截图** | 1440×3200, 2.10MB | 1440×3200, 148KB | 1440×3200, 256KB |
| **超长记录** | 1242×22080, 6.10MB | 758×13490, 290KB | 744×13129, 256KB |
| **全景横图** | 12000×5000, 8.10MB | 1440×600, 126KB | 1440×600, 123KB |
| **设计原稿** | 6000×6000, 6.90MB | 1440×1440, 263KB | 1440×1440, 279KB |

## 🔬 核心算法特性

本库采用**自适应统一图像压缩算法 (Adaptive Unified Image Compression)**，通过原图的分辨率特征，动态应用差异化策略，实现画质与体积的最优平衡。

### 智能分辨率决策

- **高清基准 (1440p)**：默认以 1440px 作为短边基准，确保在现代 2K/4K 屏幕上的视觉清晰度
- **全景墙策略**：自动识别超大全景图（长边 >10800px），锁定长边为 1440px，保留完整视野
- **超大像素陷阱**：对超过 4096万像素的超高像素图自动执行 1/4 降采样处理
- **长图内存保护**：针对超长截图建立 10.24MP 像素上限，通过等比缩放防止 OOM

### 自适应比特率控制

- **极小图 (<0.5MP)**：几乎不进行有损压缩，防止压缩伪影
- **高频信息图 (0.5-1MP)**：提高编码质量，补偿分辨率损失
- **标准图片 (1-3MP)**：应用平衡系数，对标主流社交软件体验
- **超大图/长图 (>3MP)**：应用高压缩率，显著减少体积

### 健壮性保障

- **膨胀回退**：压缩后体积大于原图时，自动透传原图，确保绝不"负优化"
- **智能格式透传**：保留小体积 PNG 的透明通道，大体积 PNG 自动转码为 JPEG
- **输入防御**：妥善处理极端分辨率输入（0、负数、1px 等），防止崩溃

# 📦 导入

确保项目的 `build.gradle` 或 `build.gradle.kts` 已配置 Maven Central 仓库：

```kotlin
repositories {
    mavenCentral()
}
```

在模块的构建文件中添加依赖：

**Kotlin DSL (`build.gradle.kts`):**

```kotlin
dependencies {
    implementation("top.zibin:luban:2.0.0")
}
```

**Groovy (`build.gradle`):**

```groovy
dependencies {
    implementation 'top.zibin:luban:2.0.0'
}
```

> 注意：请访问 [Maven Central](https://search.maven.org/search?q=g:top.zibin%20a:luban) 查看最新版本号。

# 💻 使用

### ⚡ Kotlin (Coroutines)

在 Kotlin 中，推荐使用 `suspend` 函数进行调用，代码更简洁。

#### 压缩单张图片

```kotlin
lifecycleScope.launch {
    val inputUri: Uri = ... // 图片 Uri
    val outputDir = context.cacheDir
    
    Luban.compress(context, inputUri, outputDir)
        .onSuccess { file ->
            // 压缩成功，file 为压缩后的图片文件
            Log.d("Luban", "Compressed: ${file.absolutePath}")
        }
        .onFailure { error ->
            // 处理错误
            Log.e("Luban", "Error: ${error.message}")
        }
}
```

#### 压缩单张图片文件

```kotlin
lifecycleScope.launch {
    val inputFile: File = ... 
    val outputDir = context.cacheDir
    
    Luban.compress(inputFile, outputDir)
        .onSuccess { file ->
            Log.d("Luban", "Compressed: ${file.absolutePath}")
        }
        .onFailure { error ->
            Log.e("Luban", "Error: ${error.message}")
        }
}
```

#### 压缩到指定文件路径

```kotlin
lifecycleScope.launch {
    val inputFile: File = ...
    val outputFile = File(context.cacheDir, "custom_output.jpg")
    
    Luban.compressToFile(inputFile, outputFile)
        .onSuccess { file ->
            Log.d("Luban", "Compressed to: ${file.absolutePath}")
        }
        .onFailure { error ->
            Log.e("Luban", "Error: ${error.message}")
        }
}
```

#### 并发压缩多张图片

```kotlin
lifecycleScope.launch {
    val uris: List<Uri> = ... 
    val outputDir = context.cacheDir

    val results = Luban.compress(context, uris, outputDir)
    
    results.forEach { result ->
        result.onSuccess { file -> 
            Log.d("Luban", "Compressed: ${file.absolutePath}")
        }
        .onFailure { error ->
            Log.e("Luban", "Error: ${error.message}")
        }
    }
}
```

#### 并发压缩多个文件

```kotlin
lifecycleScope.launch {
    val files: List<File> = ...
    val outputDir = context.cacheDir

    val results = Luban.compress(files, outputDir)
    
    results.forEach { result ->
        result.onSuccess { file -> 
            Log.d("Luban", "Compressed: ${file.absolutePath}")
        }
        .onFailure { error ->
            Log.e("Luban", "Error: ${error.message}")
        }
    }
}
```

#### 在其他协程作用域中使用

如果不在 Activity/Fragment 中使用，可以使用 `CoroutineScope`：

```kotlin
val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

scope.launch {
    val inputUri: Uri = ...
    val outputDir = context.cacheDir
    
    Luban.compress(context, inputUri, outputDir)
        .onSuccess { file ->
            // 处理成功
        }
        .onFailure { error ->
            // 处理错误
        }
}
```

### ☕ Java / Builder 模式

对于 Java 项目或偏好回调风格的开发者，可以使用兼容旧版风格的 `Luban.with()` API。

#### 压缩单张图片

```java
Luban.with(context)
    .load(imageFile) // 支持 File, Uri, 或 String 路径
    .setTargetDir(context.getCacheDir())
    .bindLifecycle(lifecycleOwner) // 可选：页面销毁时自动取消
    .setCompressListener(new OnCompressListener() {
        @Override
        public void onStart() {
            // 开始压缩
        }

        @Override
        public void onSuccess(File file) {
            // 压缩成功
        }

        @Override
        public void onError(Throwable e) {
            // 发生错误
        }
    })
    .launch();
```

#### 压缩多张图片

```java
List<String> imagePaths = ...; // 图片路径列表

Luban.with(context)
    .load(imagePaths) // 加载图片列表
    .setTargetDir(context.getCacheDir())
    .setCompressListener(new OnCompressListener() {
        @Override
        public void onStart() {
            // 开始压缩
        }

        @Override
        public void onSuccess(File file) {
            // 每张图片压缩成功后都会回调一次
            Log.d("Luban", "Compressed: " + file.getAbsolutePath());
        }

        @Override
        public void onError(Throwable e) {
            // 发生错误
        }
    })
    .launch();
```

# ☕ 捐助

如果这个项目对您有帮助，欢迎通过以下方式支持我的工作。您的支持是我持续改进和维护这个项目的动力。

<div align="center">

<table>
<tr>
<td align="center">
<img src="images/alipay.png" width="300" alt="支付宝收款码" />
</td>
<td width="50"></td>
<td align="center">
<img src="images/wechat.png" width="300" alt="微信收款码" />
</td>
</tr>
</table>

</div>

感谢您的支持！🙏

# 📄 License

    Copyright 2025 郑梓斌
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
