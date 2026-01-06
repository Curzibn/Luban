package top.zibin.luban.compression

class CompressionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

object TurboJpegNative {
    init {
        System.loadLibrary("turbojpeg-wrapper")
    }

    @JvmStatic
    external fun compress(
        rgbData: ByteArray,
        width: Int,
        height: Int,
        quality: Int
    ): ByteArray?

    @JvmStatic
    external fun getErrorString(): String

    fun compressOrThrow(
        rgbData: ByteArray,
        width: Int,
        height: Int,
        quality: Int
    ): ByteArray {
        val result = compress(rgbData, width, height, quality)
        if (result == null) {
            val errorMessage = getErrorString().takeIf { it.isNotBlank() }
                ?: "JPEG compression failed"
            throw CompressionException(
                "Failed to compress image (${width}x${height}, quality=$quality): $errorMessage"
            )
        }
        return result
    }
}

