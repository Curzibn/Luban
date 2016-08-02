# Luban
Luban(鲁班)——Android图片压缩工具，仿微信朋友圈压缩策略

#项目描述

目前做app开发总绕不开图片这个元素。但是随着手机拍照分辨率的提升，图片的压缩成为一个很重要的问题。单纯对图片进行裁切，压缩已经有很多文章介绍。但是裁切成多少，压缩成多少却很难控制好，裁切过头图片太小，质量压缩过头则显示效果太差。

于是自然想到app巨头“微信”会是怎么处理，Luban(鲁班)就是通过在微信朋友圈发送近100张不同分辨率图片，对比原图与微信压缩后的图片逆向推算出来的压缩算法。

因为是逆向推算，效果还没法跟微信一模一样，但是已经很接近微信朋友圈压缩后的效果，具体看以下对比！

#效果与对比

内容 | 原图 | Luban | Wechat
---|---|---|---
截屏 720P |720*1280,390k|720*1280,87k|720*1280,56k
截屏 1080P|1080*1920,2.21M|1080*1920,104k|1080*1920,112k
拍照 13M(4:3)|3096*4128,3.12M|1548*2064,141k|1548*2064,147k
拍照 9.6M(16:9)|4128*2322,4.64M|1032*581,97k|1032*581,74k
滚动截屏|1080*6433,1.56M|1080*6433,351k|1080*6433,482k

#导入
    compile 'top.zibin:Luban:1.0.3'
    
#使用
###Listener方式
因为图片压缩会有 io 耗时操作，所有采用此种方式调用请开启线程调用
    
    Luban.get(this)
        .load(File)                     //传人要压缩的图片
        .putGear(Luban.THIRD_GEAR)      //设定压缩档次，默认三挡
        .setCompressListener(new OnCompressListener() { //设置回调
            @Override
            public void onSuccess(File file) {
                //回调返回压缩后图片
            }
        }).launch();    //启动压缩
        
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
