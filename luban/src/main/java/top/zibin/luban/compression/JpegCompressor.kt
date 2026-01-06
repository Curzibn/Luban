package top.zibin.luban.compression

import android.graphics.Bitmap

class JpegCompressor : Compressor {
    override fun compress(bitmap: Bitmap, targetSizeKb: Int?, fixedQuality: Int?): ByteArray {
        val rgbData = bitmapToRgb(bitmap)
        
        if (fixedQuality != null) {
            return TurboJpegNative.compressOrThrow(rgbData, bitmap.width, bitmap.height, fixedQuality)
        }

        if (targetSizeKb == null) {
            return TurboJpegNative.compressOrThrow(rgbData, bitmap.width, bitmap.height, 60)
        }

        val low = 5
        val high = 95
        var bestData: ByteArray? = null

        val testResult = TurboJpegNative.compressOrThrow(rgbData, bitmap.width, bitmap.height, 95)
        val sizeKb = testResult.size / 1024.0

        if (sizeKb <= targetSizeKb) {
            return testResult
        }

        var currentLow = low
        var currentHigh = high

        while (currentLow <= currentHigh) {
            val mid = (currentLow + currentHigh) / 2

            val compressed = TurboJpegNative.compressOrThrow(rgbData, bitmap.width, bitmap.height, mid)
            val currentSizeKb = compressed.size / 1024.0

            if (currentSizeKb <= targetSizeKb) {
                bestData = compressed
                currentLow = mid + 1
            } else {
                currentHigh = mid - 1
            }
        }

        if (bestData == null) {
            bestData = TurboJpegNative.compressOrThrow(rgbData, bitmap.width, bitmap.height, 5)
        }

        return bestData
    }

    private fun bitmapToRgb(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val rgbData = ByteArray(width * height * 3)
        var offset = 0

        for (pixel in pixels) {
            rgbData[offset++] = ((pixel shr 16) and 0xFF).toByte()
            rgbData[offset++] = ((pixel shr 8) and 0xFF).toByte()
            rgbData[offset++] = (pixel and 0xFF).toByte()
        }

        return rgbData
    }
}
