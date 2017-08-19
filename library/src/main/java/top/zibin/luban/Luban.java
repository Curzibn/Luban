package top.zibin.luban;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Luban implements Handler.Callback {
  private static final String TAG = "Luban";
  private static final String DEFAULT_DISK_CACHE_DIR = "luban_disk_cache";

  private static final int MSG_COMPRESS_SUCCESS = 0;
  private static final int MSG_COMPRESS_START = 1;
  private static final int MSG_COMPRESS_ERROR = 2;

  private List<String> mPaths;
  private OnCompressListener onCompressListener;

  private Handler mHandler;

  private Luban(Builder builder) {
    this.mPaths = builder.mPaths;
    this.onCompressListener = builder.onCompressListener;
    mHandler = new Handler(Looper.getMainLooper(), this);
  }

  public static Builder with(Context context) {
    return new Builder(context);
  }

  /**
   * Returns a mFile with a cache audio name in the private cache directory.
   *
   * @param context
   *     A context.
   */
  private File getImageCacheFile(Context context) {
    if (getImageCacheDir(context) != null) {
      return new File(getImageCacheDir(context) + "/" + System.currentTimeMillis() + (int) (Math.random() * 1000) + ".jpg");
    }
    return null;
  }

  /**
   * Returns a directory with a default name in the private cache directory of the application to
   * use to store retrieved audio.
   *
   * @param context
   *     A context.
   *
   * @see #getImageCacheDir(Context, String)
   */
  @Nullable
  private File getImageCacheDir(Context context) {
    return getImageCacheDir(context, DEFAULT_DISK_CACHE_DIR);
  }

  /**
   * Returns a directory with the given name in the private cache directory of the application to
   * use to store retrieved media and thumbnails.
   *
   * @param context
   *     A context.
   * @param cacheName
   *     The name of the subdirectory in which to store the cache.
   *
   * @see #getImageCacheDir(Context)
   */
  @Nullable
  private File getImageCacheDir(Context context, String cacheName) {
    File cacheDir = context.getExternalCacheDir();
    if (cacheDir != null) {
      File result = new File(cacheDir, cacheName);
      if (!result.mkdirs() && (!result.exists() || !result.isDirectory())) {
        // File wasn't able to create a directory, or the result exists but not a directory
        return null;
      }
      return result;
    }
    if (Log.isLoggable(TAG, Log.ERROR)) {
      Log.e(TAG, "default disk cache dir is null");
    }
    return null;
  }

  /**
   * start asynchronous compress thread
   */
  @UiThread private void launch(final Context context) {
    if (mPaths == null || mPaths.size() == 0 && onCompressListener != null) {
      onCompressListener.onError(new NullPointerException("image file cannot be null"));
    }

    Iterator<String> iterator = mPaths.iterator();
    while (iterator.hasNext()) {
      final String path = iterator.next();
      if (Checker.isImage(path)) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
          @Override public void run() {
            try {
              mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_START));

              File result = new Engine(path, getImageCacheFile(context)).compress();
              mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_SUCCESS, result));
            } catch (IOException e) {
              mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_ERROR, e));
            }
          }
        });
      } else {
        Log.e(TAG, "can not read the path : " + path);
      }
      iterator.remove();
    }
  }

  /**
   * start compress and return the mFile
   */
  @WorkerThread private File get(String path, Context context) throws IOException {
    return new Engine(path, getImageCacheFile(context)).compress();
  }

  @WorkerThread private List<File> get(Context context) throws IOException {
    List<File> results = new ArrayList<>();
    Iterator<String> iterator = mPaths.iterator();

    while (iterator.hasNext()) {
      String path = iterator.next();
      if (Checker.isImage(path)) {
        results.add(new Engine(path, getImageCacheFile(context)).compress());
      }
      iterator.remove();
    }

    return results;
  }

  @Override public boolean handleMessage(Message msg) {
    if (onCompressListener == null) return false;

    switch (msg.what) {
      case MSG_COMPRESS_START:
        onCompressListener.onStart();
        break;
      case MSG_COMPRESS_SUCCESS:
        onCompressListener.onSuccess((File) msg.obj);
        break;
      case MSG_COMPRESS_ERROR:
        onCompressListener.onError((Throwable) msg.obj);
        break;
    }
    return false;
  }

  public static class Builder {
    private Context context;
    private List<String> mPaths;
    private OnCompressListener onCompressListener;

    Builder(Context context) {
      this.context = context;
      this.mPaths = new ArrayList<>();
    }

    private Luban build() {
      return new Luban(this);
    }

    public Builder load(File file) {
      mPaths.add(file.getAbsolutePath());
      return this;
    }

    public Builder load(String string) {
      mPaths.add(string);
      return this;
    }

    public Builder load(List<String> list) {
      mPaths.addAll(list);
      return this;
    }

    public Builder putGear(int gear) {
      return this;
    }

    public Builder setCompressListener(OnCompressListener listener) {
      this.onCompressListener = listener;
      return this;
    }

    public void launch() {
      build().launch(context);
    }

    public File get(String path) throws IOException {
      return build().get(path, context);
    }

    public List<File> get() throws IOException {
      return build().get(context);
    }
  }
}