package top.zibin.luban.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

data class ImageData(
    val bitmap: Bitmap,
    val fileSizeKb: Float
)

class ImageLoader {
    suspend fun loadFromUri(
        context: Context,
        uri: Uri,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ): ImageData = withContext(Dispatchers.IO) {
        val fileSizeKb = getFileSizeFromUri(context, uri)

        val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            decodeStream(inputStream, targetWidth, targetHeight) {
                context.contentResolver.openInputStream(uri)
            }?.let { originalBitmap ->
                val rotation = getRotationFromExifUri(context, uri)
                if (rotation != 0) {
                    rotateBitmap(originalBitmap, rotation)
                } else {
                    originalBitmap
                }
            } ?: throw IllegalArgumentException("无法解码图片 (Cannot decode image)")
        } ?: throw IllegalArgumentException("无法打开 URI (Cannot open URI): $uri")

        ImageData(bitmap, fileSizeKb)
    }

    suspend fun loadFromFile(file: File, targetWidth: Int = 0, targetHeight: Int = 0): ImageData =
        withContext(Dispatchers.IO) {
            if (!file.exists()) {
                throw IllegalArgumentException("文件不存在 (File does not exist): ${file.absolutePath}")
            }

            val fileSizeKb = file.length() / 1024.0f

            FileInputStream(file).use { inputStream ->
                decodeStream(inputStream, targetWidth, targetHeight) {
                    FileInputStream(file)
                }?.let { originalBitmap ->
                    val rotation = getRotationFromExifFile(file)
                    if (rotation != 0) {
                        rotateBitmap(originalBitmap, rotation)
                    } else {
                        originalBitmap
                    }
                } ?: throw IllegalArgumentException("无法解码图片 (Cannot decode image)")
            }.let { bitmap ->
                ImageData(bitmap, fileSizeKb)
            }
        }

    private fun decodeStream(
        inputStream: InputStream,
        targetWidth: Int,
        targetHeight: Int,
        openStream: () -> InputStream?
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        if (originalWidth <= 0 || originalHeight <= 0) {
            return null
        }

        val maxDimension = 8192
        val maxMemoryDimension = if (targetWidth > 0 && targetHeight > 0) {
            maxOf(targetWidth, targetHeight)
        } else {
            maxDimension
        }

        val finalWidth = if (targetWidth > 0) targetWidth else maxMemoryDimension
        val finalHeight = if (targetHeight > 0) targetHeight else maxMemoryDimension

        options.inSampleSize = calculateInSampleSize(options, finalWidth, finalHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        options.inDither = false
        options.inScaled = false

        return openStream()?.use { newStream ->
            var bitmap = BitmapFactory.decodeStream(newStream, null, options)
            
            if (bitmap != null && (bitmap.width > finalWidth || bitmap.height > finalHeight)) {
                val scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    finalWidth,
                    finalHeight,
                    true
                )
                if (scaledBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = scaledBitmap
                }
            }
            
            bitmap
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val pixelCount = (width.toLong() * height) / (inSampleSize * inSampleSize)
        val maxPixels = 16_000_000L
        
        if (pixelCount > maxPixels) {
            val scale = kotlin.math.sqrt(pixelCount.toDouble() / maxPixels.toDouble())
            inSampleSize = (inSampleSize * scale).toInt().coerceAtLeast(1)
        }

        return inSampleSize
    }

    private fun getFileSizeFromUri(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize / 1024.0f
            } ?: 0f
        } catch (_: Exception) {
            0f
        }
    }

    private fun getRotationFromExifFile(file: File): Int {
        return try {
            val exif = ExifInterface(file)
            getRotationFromExif(exif)
        } catch (_: Exception) {
            0
        }
    }

    private fun getRotationFromExifUri(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                getRotationFromExif(exif)
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun getRotationFromExif(exif: ExifInterface): Int {
        return when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap

        val matrix = android.graphics.Matrix().apply {
            postRotate(degrees.toFloat())
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }
        
        return rotatedBitmap
    }
}
