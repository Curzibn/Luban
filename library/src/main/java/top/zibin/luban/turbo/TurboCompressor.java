package top.zibin.luban.turbo;

import android.graphics.Bitmap;

/**
 * Author: zibin
 * Datetime: 2018/7/20
 */
public class TurboCompressor {

  public static void compress(Bitmap bitmap, int quality, String outfile) {
    nativeCompress(bitmap, quality, outfile);
  }

  static {
    System.loadLibrary("luban");
  }

  private static native boolean nativeCompress(Bitmap bitmap, int quality, String outfile);
}
