# Luban
Luban(鲁班)——Android图片压缩工具，仿微信朋友圈压缩策略

#效果与对比

内容 | 原图 | Luban | Wechat
---|---|---|---
截屏 720P |720*1280,390k|720*1280,87k|720*1280,56k
截屏 1080P|1080*1920,2.21M|1080*1920,104k|1080*1920,112k
拍照 13M(4:3)|3096*4128,3.12M|1548*2064,141k|1548*2064,147k
拍照 9.6M(16:9)|4128*2322,4.64M|1032*581,97k|1032*581,74k
滚动截屏|1080*6433,1.56M|1080*6433,351k|1080*6433,482k

#导入
    compile 'top.zibin:Luban:1.0.2'
    
#使用
    
    Luban.get(this)
        .load(File)                     //传人要压缩的图片
        .putGear(Luban.THIRD_GEAR)      //设定压缩档次，默认三挡
        .setCompressListener(new OnCompressListener() { //设置回调
            @Override
            public void onSuccess(File file) {
                //回调返回压缩后图片
            }
        }).launch();    //启动压缩
