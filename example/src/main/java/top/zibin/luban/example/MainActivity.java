package top.zibin.luban.example;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;
import top.zibin.luban.OnRenameListener;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "Luban";
  private static final int range = 3;

  private List<ImageBean> mImageList = new ArrayList<>();
  private ImageAdapter mAdapter = new ImageAdapter(mImageList);
  private CompositeDisposable mDisposable;

  private List<File> originPhotos = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mDisposable = new CompositeDisposable();

    RecyclerView mRecyclerView = findViewById(R.id.recycler_view);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mRecyclerView.setAdapter(mAdapter);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mDisposable.clear();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    originPhotos.clear();
    mImageList.clear();

    switch (item.getItemId()) {
      case R.id.sync_files:
        withRx(assetsToFiles());
        break;
      case R.id.sync_uris:
        withRx(assetsToUri());
        break;
      case R.id.async_files:
        withLs(assetsToFiles());
        break;
      case R.id.async_uris:
        withLs(assetsToUri());
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  private List<File> assetsToFiles() {
    final List<File> files = new ArrayList<>();

    for (int i = 0; i < range; i++) {
      try {
        InputStream is = getResources().getAssets().open("img_" + i);
        File file = new File(getExternalFilesDir(null), "test_" + i);
        FileOutputStream fos = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        int len = is.read(buffer);
        while (len > 0) {
          fos.write(buffer, 0, len);
          len = is.read(buffer);
        }
        fos.close();
        is.close();

        files.add(file);
        originPhotos.add(file);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return files;
  }

  private List<Uri> assetsToUri() {
    final List<Uri> uris = new ArrayList<>();
    final List<File> files = assetsToFiles();

    for (int i = 0; i < range; i++) {
      Uri uri = Uri.fromFile(files.get(i));
      uris.add(uri);
    }

    return uris;
  }

  private <T> void withRx(final List<T> photos) {
    mDisposable.add(Flowable.just(photos)
        .observeOn(Schedulers.io())
        .map(new Function<List<T>, List<File>>() {
          @Override
          public List<File> apply(@NonNull List<T> list) throws Exception {
            return Luban.with(MainActivity.this)
                .setTargetDir(getPath())
                .load(list)
                .get();
          }
        })
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError(new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) {
            Log.e(TAG, throwable.getMessage());
          }
        })
        .onErrorResumeNext(Flowable.<List<File>>empty())
        .subscribe(new Consumer<List<File>>() {
          @Override
          public void accept(@NonNull List<File> list) {
            for (File file : list) {
              Log.i(TAG, file.getAbsolutePath());
              showResult(originPhotos, file);
            }
          }
        }));
  }

  private <T> void withLs(final List<T> photos) {
    Luban.with(this)
        .load(photos)
        .ignoreBy(100)
        .setTargetDir(getPath())
        .setFocusAlpha(false)
        .filter(new CompressionPredicate() {
          @Override
          public boolean apply(String path) {
            return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
          }
        })
        .setRenameListener(new OnRenameListener() {
          @Override
          public String rename(String filePath) {
            try {
              MessageDigest md = MessageDigest.getInstance("MD5");
              md.update(filePath.getBytes());
              return new BigInteger(1, md.digest()).toString(32);
            } catch (NoSuchAlgorithmException e) {
              e.printStackTrace();
            }
            return "";
          }
        })
        .setCompressListener(new OnCompressListener() {
          @Override
          public void onStart() { }

          @Override
          public void onSuccess(File file) {
            Log.i(TAG, file.getAbsolutePath());
            showResult(originPhotos, file);
          }

          @Override
          public void onError(Throwable e) { }
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

  private void showResult(List<File> photos, File file) {
    int[] originSize = computeSize(photos.get(mAdapter.getItemCount()));
    int[] thumbSize = computeSize(file);
    String originArg = String.format(Locale.CHINA, "原图参数：%d*%d, %dk", originSize[0], originSize[1], photos.get(mAdapter.getItemCount()).length() >> 10);
    String thumbArg = String.format(Locale.CHINA, "压缩后参数：%d*%d, %dk", thumbSize[0], thumbSize[1], file.length() >> 10);

    ImageBean imageBean = new ImageBean(originArg, thumbArg, file.getAbsolutePath());
    mImageList.add(imageBean);
    mAdapter.notifyDataSetChanged();
  }

  private int[] computeSize(File srcImg) {
    int[] size = new int[2];

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    options.inSampleSize = 1;

    BitmapFactory.decodeFile(srcImg.getAbsolutePath(), options);
    size[0] = options.outWidth;
    size[1] = options.outHeight;

    return size;
  }
}
