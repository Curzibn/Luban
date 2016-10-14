# Luban

<div align="right">
<a href="../README.md">:book: 中文文档</a>
</div>

`Luban` is an image compressing tool for android with efficacy very close to that of `WeChat` Moments.

# Description

With mobile development, showing images in an app has become a very frequent task.
But with the ever increasing resolution of smartphone cameras, image compression has become a rather important concern.
Although there are already a lot of writings on the internet on the topic, a great number of possible scenarios still have to be though of, like unsuccessful compressions, too small pictures (eg. for profile pictures) or too bad image quality.

Naturally, the first idea was to see how the `WeChat`, the app giant manages this task in action. To gather data, 100 images with different resolutions were sent through `WeChat` Moments, then the compressed images were compared with the original ones. `Luban`'s foundation is the result of this analysis on `WeChat`'s compression method.
Because the process was analyzed backwards, `Luban`'s efficacy is not yet exactly the same as that of `WeChat`, but the results are already very close to what `WeChat` Moments' image compression produces - see the concrete comparison below.

# Efficacy with comparison to other tools

Content | Original picture | `Luban` | `Wechat`
------- | ---------------- | ------- | --------
720P screenshot |720*1280,390k|720*1280,87k|720*1280,56k
1080P screenshot|1080*1920,2.21M|1080*1920,104k|1080*1920,112k
13M photo (4:3)|3096*4128,3.12M|1548*2064,141k|1548*2064,147k
9.6M photo (16:9)|4128*2322,4.64M|1032*581,97k|1032*581,74k
Extended screenshot|1080*6433,1.56M|1080*6433,351k|1080*6433,482k

# Setup

```sh
compile 'io.reactivex:rxandroid:1.2.1'
compile 'io.reactivex:rxjava:1.1.6'

compile 'top.zibin:Luban:1.0.8'
```

# Usage
### Via a Listener
`Luban` internally uses the `IO` thread to perform image compression, implementations only need to specify what happens when the process finishes successfully.

```java
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
```

### With `RxJava`

With `RxJava`, more freedom is left to the programmer on controlling the process. 

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
                // TODO called when compression finishes successfully, provides compressed image
            }
        });
```

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


Translation: [_Szabolcs Pasztor_](https://github.com/spqpad)  
Last updated: Aug 8, 2016
