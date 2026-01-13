package top.zibin.luban.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import top.zibin.luban.algorithm.CompressionCalculator
import top.zibin.luban.algorithm.CompressionTarget
import top.zibin.luban.compression.Compressor
import top.zibin.luban.compression.JpegCompressor
import top.zibin.luban.io.ImageData
import top.zibin.luban.io.ImageLoader
import java.io.File
import java.io.FileOutputStream

/**
 * 图片压缩核心类。
 *
 * 使用 Kotlin 协程实现异步压缩，支持单图与多图压缩。
 *
 * Image compression core class.
 *
 * Uses Kotlin Coroutines for asynchronous compression, supporting both single and multiple image processing.
 *
 * @property compressor 图片压缩实现，默认使用 [JpegCompressor]。
 * The image compressor implementation, defaults to [JpegCompressor].

 * @property imageLoader 图片加载实现，默认使用 [ImageLoader]。
 * The image loader implementation, defaults to [ImageLoader].
 
 * @property calculator 压缩参数计算实现，默认使用 [CompressionCalculator]。
 * The compression calculator implementation, defaults to [CompressionCalculator].
 */
class Luban @JvmOverloads constructor(
    private val compressor: Compressor = JpegCompressor(),
    private val imageLoader: ImageLoader = ImageLoader(),
    private val calculator: CompressionCalculator = CompressionCalculator()
) {

    companion object {
        private val defaultInstance by lazy { Luban() }

        /**
         * 为 Java 兼容性或流式 API 使用创建一个 Builder。
         *
         * Creates a builder for Java compatibility or fluent API usage.
         *
         * @param context Android 上下文。
         * The Android Context.
         *
         * @return 一个 [LubanCompat.Builder] 实例。
         * A [LubanCompat.Builder] instance.
         */
        @JvmStatic
        fun with(context: Context): LubanCompat.Builder {
            return LubanCompat.Builder(context)
        }

        /**
         * 压缩单个 Uri 图片。
         *
         * Compresses a single image from a Uri.
         *
         * @param context 用于解析 Uri 的上下文。
         * Context required to resolve the Uri.
         *
         * @param input 需要压缩的图片 Uri。
         * The image Uri to compress.
         *
         * @param outputDir 压缩后图片的输出目录，默认为 context.cacheDir。
         * The directory where the compressed image will be saved, defaults to context.cacheDir.
         *
         * @return 包含压缩后 [File] 或异常信息的 [Result]。
         * A [Result] containing the compressed [File] or the exception if failed.
         */
        @JvmOverloads
        suspend fun compress(context: Context, input: Uri, outputDir: File = context.cacheDir): Result<File> {
            return defaultInstance.compress(context, input, outputDir)
        }

        /**
         * 压缩单个图片文件。
         *
         * Compresses a single image file。
         *
         * @param input 需要压缩的图片文件。
         * The image file to compress。
         *
         * @param outputDir 压缩后图片的输出目录。
         * The directory where the compressed image will be saved。
         *
         * @return 包含压缩后 [File] 或异常信息的 [Result]。
         * A [Result] containing the compressed [File] or the exception if failed。
         */
        suspend fun compress(input: File, outputDir: File): Result<File> {
            return defaultInstance.compress(input, outputDir)
        }

        /**
         * 将单个图片文件压缩到一个指定输出文件。
         *
         * Compresses a single image file to a specific output file.
         *
         * @param input 需要压缩的图片文件。
         * The image file to compress.
         *
         * @param output 指定的输出文件。
         * The specific output file destination.
         *
         * @return 包含压缩后 [File] 或异常信息的 [Result]。
         * A [Result] containing the compressed [File] or the exception if failed.
         */
        suspend fun compressToFile(input: File, output: File): Result<File> {
            return defaultInstance.compressToFile(input, output)
        }

        /**
         * 并发压缩多个 Uri 图片。
         *
         * Compresses a list of image Uris concurrently.
         *
         * @param context 用于解析 Uri 的上下文。
         * Context required to resolve the Uris.
         *
         * @param inputs 需要压缩的 Uri 列表。
         * The list of image Uris to compress.
         *
         * @param outputDir 压缩后图片的输出目录，默认为 context.cacheDir。
         * The directory where the compressed images will be saved, defaults to context.cacheDir.
         *
         * @return [Result] 列表，每个元素包含压缩后 [File] 或异常信息。
         * A list of [Result]s, each containing the compressed [File] or exception.
         */
        @JvmOverloads
        suspend fun compress(context: Context, inputs: List<Uri>, outputDir: File = context.cacheDir): List<Result<File>> {
            return defaultInstance.compress(context, inputs, outputDir)
        }

        /**
         * 并发压缩多个图片文件。
         *
         * Compresses a list of image files concurrently.
         *
         * @param inputs 需要压缩的图片文件列表。
         * The list of image files to compress.
         *
         * @param outputDir 压缩后图片的输出目录。
         * The directory where the compressed images will be saved.
         *
         * @return [Result] 列表，每个元素包含压缩后 [File] 或异常信息。
         * A list of [Result]s, each containing the compressed [File] or exception.
         */
        suspend fun compress(inputs: List<File>, outputDir: File): List<Result<File>> {
            return defaultInstance.compress(inputs, outputDir)
        }
    }

    /**
     * 压缩单个 Uri 图片。
     *
     * Compresses a single image from a Uri.
     *
     * @param context 用于解析 Uri 的上下文。
     * Context required to resolve the Uri.
     *
     * @param input 需要压缩的图片 Uri。
     * The image Uri to compress.
     *
     * @param outputDir 压缩后图片的输出目录。
     * The directory where the compressed image will be saved.
     *
     * @return 包含压缩后 [File] 或异常信息的 [Result]。
     * A [Result] containing the compressed [File] or the exception if failed.
     */
    suspend fun compress(context: Context, input: Uri, outputDir: File): Result<File> = runCatching {
        outputDir.mkdirs()
        val outputFile = generateOutputFileForUri(input, outputDir)
        compressInternal(context, input, outputFile)
    }

    /**
     * 压缩单个图片文件。
     *
     * Compresses a single image file.
     *
     * @param input 需要压缩的图片文件。
     * The image file to compress.
     *
     * @param outputDir 压缩后图片的输出目录。
     * The directory where the compressed image will be saved.
     *
     * @return 包含压缩后 [File] 或异常信息的 [Result]。
     * A [Result] containing the compressed [File] or the exception if failed.
     */
    suspend fun compress(input: File, outputDir: File): Result<File> = runCatching {
        val outputFile = generateOutputFile(input, outputDir)
        compressInternal(input, outputFile)
    }

    /**
     * 将单个图片文件压缩到一个指定输出文件。
     *
     * Compresses a single image file to a specific output file.
     *
     * @param input 需要压缩的图片文件。
     * The image file to compress.
     *
     * @param output 指定的输出文件。
     * The specific output file destination.
     *
     * @return 包含压缩后 [File] 或异常信息的 [Result]。
     * A [Result] containing the compressed [File] or the exception if failed.
     */
    suspend fun compressToFile(input: File, output: File): Result<File> = runCatching {
        compressInternal(input, output)
    }

    /**
     * 并发压缩多个 Uri 图片。
     *
     * Compresses a list of image Uris concurrently.
     *
     * @param context 用于解析 Uri 的上下文。
     * Context required to resolve the Uris.
     *
     * @param inputs 需要压缩的 Uri 列表。
     * The list of image Uris to compress.
     *
     * @param outputDir 压缩后图片的输出目录。
     * The directory where the compressed images will be saved.
     *
     * @return [Result] 列表，每个元素包含压缩后 [File] 或异常信息。
     * A list of [Result]s, each containing the compressed [File] or exception.
     */
    suspend fun compress(context: Context, inputs: List<Uri>, outputDir: File): List<Result<File>> = coroutineScope {
        inputs.map { uri ->
            async { compress(context, uri, outputDir) }
        }.awaitAll()
    }

    /**
     * 并发压缩多个图片文件。
     *
     * Compresses a list of image files concurrently.
     *
     * @param inputs 需要压缩的图片文件列表。
     * The list of image files to compress.
     *
     * @param outputDir 压缩后图片的输出目录。
     * The directory where the compressed images will be saved.
     *
     * @return [Result] 列表，每个元素包含压缩后 [File] 或异常信息。
     * A list of [Result]s, each containing the compressed [File] or exception.
     */
    suspend fun compress(inputs: List<File>, outputDir: File): List<Result<File>> = coroutineScope {
        inputs.map { file ->
            async { compress(file, outputDir) }
        }.awaitAll()
    }

    private suspend fun compressInternal(context: Context, input: Uri, output: File): File = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(input)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        val target = calculator.calculateTarget(options.outWidth, options.outHeight)

        val imageData = imageLoader.loadFromUri(context, input, target.width, target.height)
        processImage(imageData, output, target, null)
    }

    private suspend fun compressInternal(input: File, output: File): File = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(input.absolutePath, options)

        val target = calculator.calculateTarget(options.outWidth, options.outHeight)

        val imageData = imageLoader.loadFromFile(input, target.width, target.height)
        processImage(imageData, output, target, input)
    }

    private suspend fun processImage(imageData: ImageData, output: File, target: CompressionTarget, originalFile: File?): File = withContext(Dispatchers.IO) {
        var bitmap = imageData.bitmap
        try {
            if (bitmap.config != Bitmap.Config.RGB_565 && bitmap.config != Bitmap.Config.ARGB_8888) {
                val rgbBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
                    bitmap.copy(Bitmap.Config.RGB_565, false)
                } else {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                }
                if (rgbBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = rgbBitmap
                }
            }

            val fixedQuality = if (!target.isLongImage) 60 else null
            val compressedBytes = compressor.compress(bitmap, target.targetSizeKb, fixedQuality)

            val originalSizeBytes = (imageData.fileSizeKb * 1024).toLong()
            val compressedSizeBytes = compressedBytes.size.toLong()

            output.parentFile?.mkdirs()
            
            if (compressedSizeBytes >= originalSizeBytes && originalFile != null && originalFile.exists()) {
                originalFile.copyTo(output, overwrite = true)
            } else {
                FileOutputStream(output).use { fos ->
                    fos.write(compressedBytes)
                }
            }

            output
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun generateOutputFile(inputFile: File, outputDir: File): File {
        outputDir.mkdirs()
        val inputName = inputFile.nameWithoutExtension
        val timestamp = System.currentTimeMillis()
        return File(outputDir, "${inputName}_${timestamp}.jpg")
    }

    private fun generateOutputFileForUri(uri: Uri, outputDir: File): File {
        val fileName = uri.lastPathSegment ?: "compressed"
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val timestamp = System.currentTimeMillis()
        return File(outputDir, "${nameWithoutExt}_${timestamp}.jpg")
    }
}

suspend fun File.compressTo(outputDir: File): Result<File> {
    return Luban.compress(this, outputDir)
}

suspend fun File.compressToFile(output: File): Result<File> {
    return Luban.compressToFile(this, output)
}

suspend fun Uri.compressTo(context: Context, outputDir: File = context.cacheDir): Result<File> {
    return Luban.compress(context, this, outputDir)
}

suspend fun List<File>.compressTo(outputDir: File): List<Result<File>> {
    return Luban.compress(this, outputDir)
}

suspend fun List<Uri>.compressTo(context: Context, outputDir: File = context.cacheDir): List<Result<File>> {
    return Luban.compress(context, this, outputDir)
}

class LubanDsl internal constructor(private val context: Context?) {
    var outputDir: File? = null
    private val tasks = mutableListOf<suspend () -> Result<File>>()
    private val batchTasks = mutableListOf<suspend () -> List<Result<File>>>()

    fun compress(input: File): LubanDsl {
        tasks.add {
            val currentDir = this@LubanDsl.outputDir
            if (currentDir != null) {
                Luban.compress(input, currentDir)
            } else {
                Result.failure(IllegalStateException("Output directory is not set"))
            }
        }
        return this
    }

    fun compress(input: Uri): LubanDsl {
        val ctx = context ?: throw IllegalStateException("Context is required for Uri compression")
        tasks.add {
            val currentDir = this@LubanDsl.outputDir ?: ctx.cacheDir
            Luban.compress(ctx, input, currentDir)
        }
        return this
    }

    @JvmName("compressFiles")
    fun compress(inputs: List<File>): LubanDsl {
        batchTasks.add {
            val currentDir = this@LubanDsl.outputDir
            if (currentDir != null) {
                Luban.compress(inputs, currentDir)
            } else {
                inputs.map { Result.failure<File>(IllegalStateException("Output directory is not set")) }
            }
        }
        return this
    }

    @JvmName("compressUris")
    fun compress(inputs: List<Uri>): LubanDsl {
        val ctx = context ?: throw IllegalStateException("Context is required for Uri compression")
        batchTasks.add {
            val currentDir = this@LubanDsl.outputDir ?: ctx.cacheDir
            Luban.compress(ctx, inputs, currentDir)
        }
        return this
    }

    suspend fun execute(): List<Result<File>> {
        val results = mutableListOf<Result<File>>()
        tasks.forEach { task ->
            results.add(task())
        }
        batchTasks.forEach { batchTask ->
            results.addAll(batchTask())
        }
        return results
    }
}

suspend fun luban(context: Context, block: LubanDsl.() -> Unit): List<Result<File>> {
    return LubanDsl(context).apply(block).execute()
}

suspend fun luban(block: LubanDsl.() -> Unit): List<Result<File>> {
    return LubanDsl(null).apply(block).execute()
}
