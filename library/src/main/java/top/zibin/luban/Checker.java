package top.zibin.luban;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class Checker {
  private static List<String> format = new ArrayList<>();
  static final String JPG = "jpg";
  static final String JPEG = "jpeg";
  static final String PNG = "png";
  static final String WEBP = "webp";
  static final String GIF = "gif";

  static {
    format.add(JPG);
    format.add(JPEG);
    format.add(PNG);
    format.add(WEBP);
    format.add(GIF);
  }

  static boolean isImage(String path) {
    if (TextUtils.isEmpty(path)) {
      return false;
    }

    String suffix = path.substring(path.lastIndexOf(".") + 1, path.length());
    return format.contains(suffix.toLowerCase());
  }

  static boolean isJPG(String path) {
    if (TextUtils.isEmpty(path)) {
      return false;
    }

    String suffix = path.substring(path.lastIndexOf("."), path.length());
    return suffix.contains(JPG) || suffix.contains(JPEG);
  }
}
