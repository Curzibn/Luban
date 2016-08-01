package top.zibin.luban;

import java.io.File;

public interface OnCompressListener {
    void onSuccess(File file);

    void onError(Exception e);
}
