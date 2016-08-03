package top.zibin.luban.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

import me.iwf.photopicker.PhotoPicker;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class MainActivity extends AppCompatActivity {

    private TextView fileSize;
    private TextView imageSize;
    private TextView thumbFileSize;
    private TextView thumbImageSize;
    private ImageView image;

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
     * 压缩单张图片
     */
    private void compressImage(final File file) {
        Luban.get(this)
                .load(file)
                .putGear(Luban.THIRD_GEAR)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        Toast.makeText(MainActivity.this, "I'm begin", Toast.LENGTH_SHORT).show();

                        fileSize.setText(file.length() / 1024 + "k");
                        imageSize.setText(Luban.get(MainActivity.this).getImageSize(file.getPath())[0] + " * " + Luban.get(MainActivity.this).getImageSize(file.getPath())[1]);
                    }

                    @Override
                    public void onSuccess(File file) {
                        Glide.with(MainActivity.this).load(file).into(image);

                        thumbFileSize.setText(file.length() / 1024 + "k");
                        thumbImageSize.setText(Luban.get(getApplicationContext()).getImageSize(file.getPath())[0] + " * " + Luban.get(getApplicationContext()).getImageSize(file.getPath())[1]);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }
                }).launch();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE) {
            if (data != null) {
                ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);

                compressImage(new File(photos.get(0)));
            }
        }
    }
}
