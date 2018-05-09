package top.zibin.luban.example;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.iwf.photopicker.PhotoPicker;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "Luban";

  private List<ImageBean> mImageList = new ArrayList<>();
  private ImageAdapter mAdapter = new ImageAdapter(mImageList);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mRecyclerView.setAdapter(mAdapter);

    Button fab = (Button) findViewById(R.id.fab);
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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE) {
      if (data != null) {
        mImageList.clear();

        ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
//        compressWithLs(photos);
        compressWithRx(photos);
      }
    }
  }

  private void compressWithRx(final List<String> photos) {
    Flowable.just(photos)
        .observeOn(Schedulers.io())
        .map(new Function<List<String>, List<File>>() {
          @Override public List<File> apply(@NonNull List<String> list) throws Exception {
            return Luban.with(MainActivity.this).load(list).get();
          }
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<List<File>>() {
          @Override public void accept(@NonNull List<File> list) throws Exception {
            for (File file : list) {
            showResult(photos, file);
          }
          }
        });
  }

  /**
   * 压缩图片 Listener 方式
   */
  private void compressWithLs(final List<String> photos) {
    Luban.with(this)
        .load(photos)
        .ignoreBy(100)
        .setTargetDir(getPath())
        .setCompressListener(new OnCompressListener() {
          @Override
          public void onStart() {
          }

          @Override
          public void onSuccess(File file) {
            showResult(photos, file);
          }

          @Override
          public void onError(Throwable e) {
          }
        }).launch();
  }

  private String getPath() {
    String path = Environment.getExternalStorageDirectory() + "/Luban/image/";
    File file = new File(path);
    if (file.mkdirs()) {
      return path;
    }
    return path;
  }

  private void showResult(List<String> photos, File file) {
    int[] originSize = computeSize(photos.get(mAdapter.getItemCount()));
    int[] thumbSize = computeSize(file.getAbsolutePath());
    String originArg = String.format(Locale.CHINA, "原图参数：%d*%d, %dk", originSize[0], originSize[1], new File(photos.get(mAdapter.getItemCount())).length() >> 10);
    String thumbArg = String.format(Locale.CHINA, "压缩后参数：%d*%d, %dk", thumbSize[0], thumbSize[1], file.length() >> 10);

    ImageBean imageBean = new ImageBean(originArg, thumbArg, file.getAbsolutePath());
    mImageList.add(imageBean);
    mAdapter.notifyDataSetChanged();
  }

  private int[] computeSize(String srcImg) {
    int[] size = new int[2];

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    options.inSampleSize = 1;

    BitmapFactory.decodeFile(srcImg, options);
    size[0] = options.outWidth;
    size[1] = options.outHeight;

    return size;
  }
}
