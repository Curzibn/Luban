package top.zibin.luban.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;

import me.iwf.photopicker.PhotoPicker;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import top.zibin.luban.GearMode;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class MainActivity extends AppCompatActivity {

    private TextView fileSize;
    private TextView imageSize;
    private TextView thumbFileSize;
    private TextView thumbImageSize;
    private TextView originPath;
    private TextView destPath;
    private TextView destPath2;
    private ImageView image;
    private File destFile;
    private File destFile2;
    private ImageView image2;
    private TextView thumbFileSize2;
    private TextView thumbImageSize2;

    private TextView tv_mode1;
    private TextView tv_mode2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fileSize = (TextView) findViewById(R.id.file_size);
        imageSize = (TextView) findViewById(R.id.image_size);

        thumbFileSize = (TextView) findViewById(R.id.thumb_file_size);
        thumbImageSize = (TextView) findViewById(R.id.thumb_image_size);
        thumbFileSize2 = (TextView) findViewById(R.id.thumb_file_size2);
        thumbImageSize2 = (TextView) findViewById(R.id.thumb_image_size2);

        image = (ImageView) findViewById(R.id.image_first);
        image2 = (ImageView) findViewById(R.id.image_second);
        originPath = (TextView) findViewById(R.id.tv_ori_path);
        destPath = (TextView) findViewById(R.id.tv_dest_path);
        destPath2 = (TextView) findViewById(R.id.tv_dest_path2);

        tv_mode1 = (TextView) findViewById(R.id.gear_mode);
        tv_mode2 = (TextView) findViewById(R.id.gear_mode2);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoPicker.builder()
                        .setPhotoCount(1)
                        .setShowCamera(true)
                        .setShowGif(true)
                        .setPreviewEnabled(false)
                        .start(MainActivity.this, PhotoPicker.REQUEST_CODE);

            }
        });
    }

    /**
     * 压缩单张图片 Listener 方式
     */
    private void compressWithLs(final File file_ori,
                                @GearMode
                                final int mode) {

        Luban.get(this)
                .from(file_ori)
                .putGear(mode)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess(File file) {
                        showResult(file, mode, file_ori);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();
    }

    /**
     * 压缩单张图片 RxJava 方式
     */
    private void compressWithRx(final File file_ori,
                                @GearMode
                                final int mode) {
        Luban.get(this)
                .from(file_ori)
                .to(destFile)
                .putGear(mode)
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
                        showResult(file, mode, file_ori);

                    }
                });
    }

    private void showResult(File file,
                            @GearMode
                            int mode, File file_ori) {
        switch (mode) {

            case Luban.FIRST_GEAR:
                Glide.with(MainActivity.this).load(file).into(image);
                originPath.setText(file_ori.getAbsolutePath());
                destPath.setText(file.getAbsolutePath());
                thumbFileSize.setText(file.length() / 1024 + "k");
                thumbImageSize.setText(Luban.get(getApplicationContext()).getImageSize(file.getPath())[0] + " * " + Luban.get(getApplicationContext()).getImageSize(file.getPath())[1]);
                tv_mode1.setText("一档");
                break;
            case Luban.THIRD_GEAR:
                Glide.with(MainActivity.this).load(file).into(image2);
                destPath2.setText(file.getAbsolutePath());
                thumbFileSize2.setText(file.length() / 1024 + "k");
                thumbImageSize2.setText(Luban.get(getApplicationContext()).getImageSize(file.getPath())[0] + " * " + Luban.get(getApplicationContext()).getImageSize(file.getPath())[1]);
                tv_mode2.setText("三档");
                break;


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE) {
            if (data != null) {
                ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);

                File imgFile = new File(photos.get(0));
                fileSize.setText(imgFile.length() / 1024 + "k");
                imageSize.setText(Luban.get(this).getImageSize(imgFile.getPath())[0] + " * " + Luban.get(this).getImageSize(imgFile.getPath())[1]);

                //测试区别
                compressWithRx(new File(photos.get(0)),Luban.FIRST_GEAR);
                compressWithRx(new File(photos.get(0)),Luban.THIRD_GEAR);

//                compressWithLs(new File(photos.get(0)), Luban.FIRST_GEAR);
//                compressWithLs(new File(photos.get(0)), Luban.THIRD_GEAR);
            }
        }
    }
}
