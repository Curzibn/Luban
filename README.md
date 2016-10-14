# Luban

<div align="right">
<a href="Translation/README-EN.md">:book: English Documentation</a>
</div>

`Luban`（鲁班） —— `Android`图片压缩工具，仿微信朋友圈压缩策略。

#项目描述

目前做`App`开发总绕不开图片这个元素。但是随着手机拍照分辨率的提升，图片的压缩成为一个很重要的问题。单纯对图片进行裁切，压缩已经有很多文章介绍。但是裁切成多少，压缩成多少却很难控制好，裁切过头图片太小，质量压缩过头则显示效果太差。

于是自然想到`App`巨头“微信”会是怎么处理，`Luban`（鲁班）就是通过在微信朋友圈发送近100张不同分辨率图片，对比原图与微信压缩后的图片逆向推算出来的压缩算法。

因为有其他语言也想要实现`Luban`，所以描述了一遍[算法步骤](/DESCRIPTION.md)。

因为是逆向推算，效果还没法跟微信一模一样，但是已经很接近微信朋友圈压缩后的效果，具体看以下对比！

#效果与对比

内容 | 原图 | `Luban` | `Wechat`
---- | ---- | ------ | ------
截屏 720P |720*1280,390k|720*1280,87k|720*1280,56k
截屏 1080P|1080*1920,2.21M|1080*1920,104k|1080*1920,112k
拍照 13M(4:3)|3096*4128,3.12M|1548*2064,141k|1548*2064,147k
拍照 9.6M(16:9)|4128*2322,4.64M|1032*581,97k|1032*581,74k
滚动截屏|1080*6433,1.56M|1080*6433,351k|1080*6433,482k

#导入

```sh
compile 'io.reactivex:rxandroid:1.2.1'
compile 'io.reactivex:rxjava:1.1.6'

compile 'top.zibin:Luban:1.0.9'
```

#Release Notes

### [v1.0.9, 2016/10/14](https://github.com/Curzibn/Luban/milestone/1)

- 修改压缩后文件自带后缀，根据([#77](https://github.com/Curzibn/Luban/issues/77))提供的思路

#使用

###Listener方式

`Luban`内部采用`IO`线程进行图片压缩，外部调用只需设置好结果监听即可：

```java
Luban.get(this)
    .load(File)                     //传人要压缩的图片
    .putGear(Luban.THIRD_GEAR)      //设定压缩档次，默认三挡
    .setCompressListener(new OnCompressListener() { //设置回调

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
            // TODO 当压缩过去出现问题时调用
        }
    }).launch();    //启动压缩
```

### `RxJava`方式

`RxJava`调用方式请自行随意控制线程：

```java
Luban.get(this)
        .load(file)
        .putGear(Luban.THIRD_GEAR)
        .asObservable()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        })
        .onErrorResumeNext(new Func1<Throwable, Observable<? extends File>>() {
            @Override
            public Observable<? extends File> call(Throwable throwable) {
                return Observable.empty();
            }
        })
        .subscribe(new Action1<File>() {
            @Override
            public void call(File file) {
                // TODO 压缩成功后调用，返回压缩后的图片文件
            }
        }).launch();    //启动压缩
```

###方法对应表

方法名|功能
---|---
load(File file)|传入要压缩的文件
setFilename(String filename)|设置压缩后图片命名
putGear(int gear)|设置压缩档次

#License


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
