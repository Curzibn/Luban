package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.SystemClock;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Responsible for starting compress and managing active and cached resources.
 */
class Engine {
  private ExifInterface srcExif;
  private String srcImg;
  private File tagImg;
  private int srcWidth;
  private int srcHeight;

  Engine(String srcImg, File tagImg) throws IOException {
    if (Checker.isJPG(srcImg)) {
      this.srcExif = new ExifInterface(srcImg);
    }
    this.tagImg = tagImg;
    this.srcImg = srcImg;

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    options.inSampleSize = 1;

    BitmapFactory.decodeFile(srcImg, options);
    this.srcWidth = options.outWidth;
    this.srcHeight = options.outHeight;
  }

  private int computeSize() {
    srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
    srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

    if (srcWidth < srcHeight) {
      return getInSampleSize(srcWidth);
    } else {
      return getInSampleSize(srcHeight);
    }
  }

  private int getInSampleSize(int i) {
    float ratio = i / 1080;
    if (ratio > 1.5f && ratio <= 3) {
      return 1 << 1;
    } else if (ratio > 3) {
      return 1 << 2;
    } else {
      return 1;
    }
  }

  private Bitmap rotatingImage(Bitmap bitmap) {
    if (srcExif == null) return bitmap;

    Matrix matrix = new Matrix();
    int angle = 0;
    int orientation = srcExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    switch (orientation) {
      case ExifInterface.ORIENTATION_ROTATE_90:
        angle = 90;
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        angle = 180;
        break;
      case ExifInterface.ORIENTATION_ROTATE_270:
        angle = 270;
        break;
    }

    matrix.postRotate(angle);

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
  }

  File compress() throws IOException {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = computeSize();

    Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg, options);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    tagBitmap = rotatingImage(tagBitmap);
    tagBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
    tagBitmap.recycle();

    FileOutputStream fos = new FileOutputStream(tagImg);
    fos.write(stream.toByteArray());
    fos.flush();
    fos.close();
    stream.close();

    return tagImg;
  }
}