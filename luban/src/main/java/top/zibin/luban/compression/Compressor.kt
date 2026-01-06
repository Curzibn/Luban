package top.zibin.luban.compression

import android.graphics.Bitmap

interface Compressor {
    fun compress(bitmap: Bitmap, targetSizeKb: Int?, fixedQuality: Int? = null): ByteArray
}
