package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Responsible for starting compress and managing active and cached resources.
 */
class Engine {
  private ExifInterface srcExif;
  private File srcImg;
  private File tagImg;
  private int srcWidth;
  private int srcHeight;

  Engine(File srcImg, File tagImg) throws IOException {
    if (isJpeg(srcImg)) {
      this.srcExif = new ExifInterface(srcImg.getAbsolutePath());
    }
    this.tagImg = tagImg;
    this.srcImg = srcImg;

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    options.inSampleSize = 1;

    BitmapFactory.decodeFile(srcImg.getAbsolutePath(), options);
    this.srcWidth = options.outWidth;
    this.srcHeight = options.outHeight;
  }

  private boolean isJpeg(File photo) {
    return photo.getAbsolutePath().contains("jpeg") || photo.getAbsolutePath().contains("jpg");
  }

  private int computeSize() {
    int mSampleSize;

    srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
    srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

    srcWidth = srcWidth > srcHeight ? srcHeight : srcWidth;
    srcHeight = srcWidth > srcHeight ? srcWidth : srcHeight;

    double scale = ((double) srcWidth / srcHeight);

    if (scale <= 1 && scale > 0.5625) {
      if (srcHeight < 1664) {
        mSampleSize = 1;
      } else if (srcHeight >= 1664 && srcHeight < 4990) {
        mSampleSize = 2;
      } else if (srcHeight >= 4990 && srcHeight < 10240) {
        mSampleSize = 4;
      } else {
        mSampleSize = srcHeight / 1280 == 0 ? 1 : srcHeight / 1280;
      }
    } else if (scale <= 0.5625 && scale > 0.5) {
      mSampleSize = srcHeight / 1280 == 0 ? 1 : srcHeight / 1280;
    } else {
      mSampleSize = (int) Math.ceil(srcHeight / (1280.0 / scale));
    }

    return mSampleSize;
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

    Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg.getAbsolutePath(), options);
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