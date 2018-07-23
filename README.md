# Luban

[![Build Status](https://travis-ci.org/Curzibn/Luban.svg?branch=turbo)](https://travis-ci.org/Curzibn/Luban)
[ ![Download](https://api.bintray.com/packages/curzibn/maven/Luban-turbo/images/download.svg) ](https://bintray.com/curzibn/maven/Luban/_latestVersion)

`Luban`（鲁班） —— `Android`图片压缩工具，仿微信朋友圈压缩策略。

# 分支描述

本分支为`Luban`主体项目引入`libjpeg-turbo`的`jni`版本

# 导入

```sh
implementation 'top.zibin:Luban-turbo:1.0.0'
```

# 使用

### 引入JNI动态文件

- `build.gradle`添加`ndk`配置
```
android {
    defaultConfig {
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a' //or x86、x86_64
        }
    }
}
```

- 拷贝`so`文件到项目`jniLibs`文件夹

Luban 提供4个平台的`so`文件: 
[`armeabi-v7a`](https://github.com/Curzibn/Luban/blob/turbo/library/src/main/jniLibs/armeabi-v7a/libluban.so)、
[`arm64-v8a`](https://github.com/Curzibn/Luban/blob/turbo/library/src/main/jniLibs/arm64-v8a/libluban.so)、
[`x86`](https://github.com/Curzibn/Luban/blob/turbo/library/src/main/jniLibs/x86/libluban.so)、
[`x86_64`](https://github.com/Curzibn/Luban/blob/turbo/library/src/main/jniLibs/x86_64/libluban.so)

### 方法列表

方法 | 描述
---- | ----
load | 传入原图
filter | 设置开启压缩条件
ignoreBy | 不压缩的阈值，单位为K
setFocusAlpha | 设置是否保留透明通道 
setTargetDir | 缓存压缩图片路径
setCompressListener | 压缩回调接口
setRenameListener | 压缩前重命名接口

### 异步调用

`Luban`内部采用`IO`线程进行图片压缩，外部调用只需设置好结果监听即可：

```java
Luban.with(this)
        .load(photos)
        .ignoreBy(100)
        .setTargetDir(getPath())
        .filter(new CompressionPredicate() {
          @Override
          public boolean apply(String path) {
            return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
          }
        })
        .setCompressListener(new OnCompressListener() {
          @Override
          public void onStart() {
            // TODO 压缩开始前调用，可以在方法内启动 loading UI
          }

          @Override
          public void onSuccess(File file) {
            // TODO 压缩成功后调用，返回压缩后的图片文件
          }

          @Override
          public void onError(Throwable e) {
            // TODO 当压缩过程出现问题时调用
          }
        }).launch();
```

### 同步调用

同步方法请尽量避免在主线程调用以免阻塞主线程，下面以rxJava调用为例

```java
Flowable.just(photos)
    .observeOn(Schedulers.io())
    .map(new Function<List<String>, List<File>>() {
      @Override public List<File> apply(@NonNull List<String> list) throws Exception {
        // 同步方法直接返回压缩后的文件
        return Luban.with(MainActivity.this).load(list).get();
      }
    })
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe();
```

### RELEASE NOTE

[Here](https://github.com/Curzibn/Luban/releases)

# License

    Copyright 2016 Zheng Zibin
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
