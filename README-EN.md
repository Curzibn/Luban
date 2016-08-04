# Intro
Luban(鲁班)——Android图片压缩工具，仿微信朋友圈压缩策略
Luban is an android image compressing tool. 

# Description

目前做app开发总绕不开图片这个元素。但是随着手机拍照分辨率的提升，图片的压缩成为一个很重要的问题。单纯对图片进行裁切，压缩已经有很多文章介绍。但是裁切成多少，压缩成多少却很难控制好，裁切过头图片太小，质量压缩过头则显示效果太差。

于是自然想到app巨头“微信”会是怎么处理，Luban(鲁班)就是通过在微信朋友圈发送近100张不同分辨率图片，对比原图与微信压缩后的图片逆向推算出来的压缩算法。

因为是逆向推算，效果还没法跟微信一模一样，但是已经很接近微信朋友圈压缩后的效果，具体看以下对比！

# Efficacy in comparison to other tools

Content | Original picture | Luban | Wechat
---|---|---|---
720P screenshot |720*1280,390k|720*1280,87k|720*1280,56k
1080P screenshot|1080*1920,2.21M|1080*1920,104k|1080*1920,112k
13M photo (4:3)|3096*4128,3.12M|1548*2064,141k|1548*2064,147k
9.6M photo (16:9)|4128*2322,4.64M|1032*581,97k|1032*581,74k
Extended screenshot|1080*6433,1.56M|1080*6433,351k|1080*6433,482k

# Setup
    compile 'top.zibin:Luban:1.0.5'
    
# Usage
### Via a Listener
Luban internally uses the IO thread to perform image compression, implementations only need to specify what happens when the process finishes successfully.
    
    Luban.get(this)
        .load(File)                     // pass image to be compressed
        .putGear(Luban.THIRD_GEAR)      // set compression level, defaults to 3
        .setCompressListener(new OnCompressListener() { // Set up return
        
            @Override
            public void onStart() {
                // TODO Called when compression starts, display loading UI here
            }
            @Override
            public void onSuccess(File file) {
                // TODO Called when compression finishes successfully, provides compressed image
            }
            
            @Override
            public void onError(Throwable e) {
                // TODO Called if an error has been encountered while compressing
            }
        }).launch();    // Start compression
        
### With RxJava
With RxJava, more freedom is left to the programmer on controlling the process. 
    
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
                    // TODO called when compression finishes successfully, provides compressed image
                }
            });

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
