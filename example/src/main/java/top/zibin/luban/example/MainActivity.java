package top.zibin.luban.example;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.iwf.photopicker.PhotoPicker;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class MainActivity extends AppCompatActivity {

    private TextView fileSize;
    private TextView imageSize;
    private TextView thumbFileSize;
    private TextView thumbImageSize;
    private ImageView image;
    private RecyclerView imageRecyclerView;
    private ImageAdapter imageAdapter;
    private List<File> mOriginalFileList = new ArrayList<>();
    private List<File> mCompressedFileList = new ArrayList<>();

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
        image = (ImageView) findViewById(R.id.image);
        imageRecyclerView = (RecyclerView) findViewById(R.id.image_recycler_view);

        imageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(mCompressedFileList);
        imageRecyclerView.setAdapter(imageAdapter);
        imageRecyclerView.setHasFixedSize(true);
        imageAdapter.setClickListener(new ImageAdapter.ImageClickListener() {
            @Override
            public void onImageClick(int position) {
                showImageFileSize(position);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoPicker.builder()
                        .setPhotoCount(9)
                        .setShowCamera(true)
                        .setShowGif(true)
                        .setPreviewEnabled(false)
                        .start(MainActivity.this, PhotoPicker.REQUEST_CODE);

            }
        });
    }

    private void showImageFileSize(int position) {
        fileSize.setText(mOriginalFileList.get(position).length() / 1024 + "k");
        imageSize.setText(Luban.get(this).getImageSize(mOriginalFileList.get(position).getPath())[0] + " * " + Luban.get(this).getImageSize(mOriginalFileList.get(position).getPath())[1]);

        thumbFileSize.setText(mCompressedFileList.get(position).length() / 1024 + "k");
        thumbImageSize.setText(Luban.get(getApplicationContext()).getImageSize(mCompressedFileList.get(position).getPath())[0] + " * " + Luban.get(getApplicationContext()).getImageSize(mCompressedFileList.get(position).getPath())[1]);
    }

    /**
     * 压缩单张图片 Listener 方式
     */
    private void compressWithLs(File file) {
        Luban.get(this)
                .load(file)
                .putGear(Luban.THIRD_GEAR)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        Toast.makeText(MainActivity.this, "I'm start", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.i("path", file.getAbsolutePath());

                        Glide.with(MainActivity.this).load(file).into(image);

                        image.setVisibility(View.VISIBLE);
                        imageRecyclerView.setVisibility(View.GONE);

                        thumbFileSize.setText(file.length() / 1024 + "k");
                        thumbImageSize.setText(Luban.get(getApplicationContext()).getImageSize(file.getPath())[0] + " * " + Luban.get(getApplicationContext()).getImageSize(file.getPath())[1]);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();
    }

    /**
     * 压缩单张图片 RxJava 方式
     */
    private void compressWithRx(File file) {
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
                        Glide.with(MainActivity.this).load(file).into(image);

                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri uri = Uri.fromFile(file);
                        intent.setData(uri);
                        MainActivity.this.sendBroadcast(intent);

                        thumbFileSize.setText(file.length() / 1024 + "k");
                        thumbImageSize.setText(Luban.get(getApplicationContext()).getImageSize(file.getPath())[0] + " * " + Luban.get(getApplicationContext()).getImageSize(file.getPath())[1]);
                    }
                });
    }

    /**
     * 压缩多张图片 RxJava 方式
     */
    private void compressWithRx(List<File> fileList) {
        Luban.get(this)
                .load(fileList)
                .putGear(Luban.THIRD_GEAR)
                .asList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends List<File>>>() {
                    @Override
                    public Observable<? extends List<File>> call(Throwable throwable) {
                        return Observable.empty();
                    }
                })
                .subscribe(new Action1<List<File>>() {
                    @Override
                    public void call(List<File> fileList) {

                        image.setVisibility(View.GONE);
                        imageRecyclerView.setVisibility(View.VISIBLE);

                        mCompressedFileList.clear();
                        mCompressedFileList.addAll(fileList);
                        imageAdapter.notifyDataSetChanged();

                        showImageFileSize(0);
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE) {
            if (data != null) {
                ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
                if (photos != null && photos.size() != 0) {
                    if (photos.size() > 1) {
                        mOriginalFileList.clear();
                        for (String filePath : photos) {
                            mOriginalFileList.add(new File(filePath));
                        }
                        compressWithRx(mOriginalFileList);
                    } else {
                        File imgFile = new File(photos.get(0));
                        fileSize.setText(imgFile.length() / 1024 + "k");
                        imageSize.setText(Luban.get(this).getImageSize(imgFile.getPath())[0] + " * " + Luban.get(this).getImageSize(imgFile.getPath())[1]);
                        compressWithLs(imgFile);
                    }
                }
            }
        }
    }
}
