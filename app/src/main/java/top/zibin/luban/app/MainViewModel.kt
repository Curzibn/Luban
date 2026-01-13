package top.zibin.luban.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.zibin.luban.api.Luban
import top.zibin.luban.api.luban
import top.zibin.luban.algorithm.CompressionCalculator
import top.zibin.luban.algorithm.CompressionTarget
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

data class ImageInfo(
    val uri: Uri? = null,
    val file: File? = null,
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0
)

data class BatchCompressResult(
    val total: Int,
    val success: Int,
    val failed: Int,
    val outputDir: File?
)

data class MainUiState(
    val originalImage: ImageInfo? = null,
    val compressedImage: ImageInfo? = null,
    val compressionTarget: CompressionTarget? = null,
    val isCompressing: Boolean = false,
    val error: String? = null,
    val showImageViewer: Boolean = false,
    val viewingImage: ImageInfo? = null,
    val isBatchCompressing: Boolean = false,
    val batchProgress: Int = 0,
    val batchTotal: Int = 0,
    val batchResult: BatchCompressResult? = null,
    val appStorageDir: String? = null,
    val outputStorageDir: String? = null
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private val calculator = CompressionCalculator()

    fun onImageSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileSize = context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, options)
                }

                val target = calculator.calculateTarget(options.outWidth, options.outHeight)

                _uiState.update {
                    it.copy(
                        originalImage = ImageInfo(
                            uri = uri,
                            size = fileSize,
                            width = options.outWidth,
                            height = options.outHeight
                        ),
                        compressedImage = null,
                        compressionTarget = target,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "选择图片失败: ${e.message}") }
            }
        }
    }

    fun compressImage(context: Context) {
        val originalUri = _uiState.value.originalImage?.uri ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isCompressing = true, error = null) }

            val outputDir = File(context.cacheDir, "luban_compressed")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val results = luban(context) {
                this.outputDir = outputDir
                compress(originalUri)
            }

            val result = results.firstOrNull()
                ?: Result.failure(IllegalStateException("No compression result"))

            result.getOrElse { error ->
                _uiState.update {
                    it.copy(isCompressing = false, error = "压缩失败: ${error.message}")
                }
                return@launch
            }.let { file ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)

                _uiState.update {
                    it.copy(
                        isCompressing = false,
                        compressedImage = ImageInfo(
                            file = file,
                            size = file.length(),
                            width = options.outWidth,
                            height = options.outHeight
                        )
                    )
                }
            }
        }
    }

    fun openImageViewer(imageInfo: ImageInfo) {
        _uiState.update { it.copy(showImageViewer = true, viewingImage = imageInfo) }
    }

    fun closeImageViewer() {
        _uiState.update { it.copy(showImageViewer = false, viewingImage = null) }
    }

    fun initStorageDirs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val appStorageDir = getAppStorageDir(context)
            val outputStorageDir = getOutputStorageDir(context)

            appStorageDir?.let { dir ->
                copyAssetsToStorage(context, dir)
            }

            _uiState.update {
                it.copy(
                    appStorageDir = appStorageDir?.absolutePath,
                    outputStorageDir = outputStorageDir?.absolutePath
                )
            }
        }
    }

    private fun copyAssetsToStorage(context: Context, targetDir: File) {
        try {
            val assetManager = context.assets
            val assetFiles = assetManager.list("test_images") ?: emptyArray()

            if (assetFiles.isEmpty()) {
                Log.d("AssetCopy", "assets/test_images 目录为空，跳过复制")
                return
            }

            val imageExtensions = setOf(".jpg", ".jpeg", ".png", ".webp", ".bmp")

            assetFiles.forEach { fileName ->
                val lowerFileName = fileName.lowercase()

                val isImageFile = imageExtensions.any { lowerFileName.endsWith(it) }
                if (!isImageFile) {
                    Log.d("AssetCopy", "跳过非图片文件: $fileName")
                    return@forEach
                }

                val targetFile = File(targetDir, fileName)

                if (targetFile.exists()) {
                    Log.d("AssetCopy", "文件已存在，跳过: $fileName")
                    return@forEach
                }

                try {
                    assetManager.open("test_images/$fileName").use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d("AssetCopy", "复制成功: $fileName")
                } catch (e: Exception) {
                    Log.e("AssetCopy", "复制失败: $fileName", e)
                }
            }
        } catch (e: Exception) {
            Log.e("AssetCopy", "复制assets图片失败", e)
        }
    }

    private fun getAppStorageDir(context: Context): File? {
        val externalFilesDir = context.getExternalFilesDir(null)?.parentFile
        return externalFilesDir?.let {
            val appDir = File(it, "images")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            appDir
        }
    }

    private fun getOutputStorageDir(context: Context): File? {
        val externalFilesDir = context.getExternalFilesDir(null)?.parentFile
        return externalFilesDir?.let {
            val outputDir = File(it, "compressed")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            outputDir
        }
    }

    fun batchCompressImages(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val inputDir = getAppStorageDir(context)
            val outputDir = getOutputStorageDir(context)

            if (inputDir == null || outputDir == null) {
                _uiState.update {
                    it.copy(error = "无法访问存储目录，请检查权限")
                }
                return@launch
            }

            if (!inputDir.exists()) {
                _uiState.update {
                    it.copy(error = "输入目录不存在: ${inputDir.absolutePath}")
                }
                return@launch
            }

            val imageFiles = inputDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".jpg", ignoreCase = true)
                        || file.name.endsWith(".jpeg", ignoreCase = true)
                        || file.name.endsWith(".png", ignoreCase = true))
            } ?: emptyArray()

            if (imageFiles.isEmpty()) {
                _uiState.update {
                    it.copy(error = "输入目录中没有找到图片文件: ${inputDir.absolutePath}")
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isBatchCompressing = true,
                    batchProgress = 0,
                    batchTotal = imageFiles.size,
                    batchResult = null,
                    error = null
                )
            }

            val inputList = imageFiles.toList()
            Log.d("BatchCompress", "开始批量压缩，共 ${inputList.size} 张图片")

            val results = luban(context) {
                this.outputDir = outputDir
                compress(inputList)
            }

            var successCount = 0
            var failedCount = 0

            results.forEachIndexed { index, result ->
                val inputFile = inputList[index]

                result.getOrNull()?.let { compressedFile ->
                    successCount++

                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(compressedFile.absolutePath, options)

                    val width = options.outWidth
                    val height = options.outHeight
                    val fileSize = compressedFile.length()
                    val fileSizeKB = fileSize / 1024.0
                    val fileSizeMB = fileSizeKB / 1024.0

                    val sizeStr = when {
                        fileSizeMB >= 1.0 -> String.format("%.2f MB", fileSizeMB)
                        else -> String.format("%.2f KB", fileSizeKB)
                    }

                    Log.d(
                        "BatchCompress",
                        "[${index + 1}/${inputList.size}] 压缩成功: " +
                                "文件名=${compressedFile.name}, " +
                                "分辨率=${width}×${height}, " +
                                "文件大小=${sizeStr} (${fileSize} bytes)"
                    )
                } ?: run {
                    failedCount++
                    val error = result.exceptionOrNull()
                    Log.e(
                        "BatchCompress",
                        "[${index + 1}/${inputList.size}] 压缩失败: ${inputFile.name}, 错误=${error?.message}"
                    )
                }

                _uiState.update {
                    it.copy(batchProgress = index + 1)
                }
            }

            Log.d(
                "BatchCompress",
                "批量压缩完成: 总计=${inputList.size}, 成功=${successCount}, 失败=${failedCount}"
            )

            _uiState.update {
                it.copy(
                    isBatchCompressing = false,
                    batchResult = BatchCompressResult(
                        total = inputList.size,
                        success = successCount,
                        failed = failedCount,
                        outputDir = outputDir
                    )
                )
            }
        }
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(
            Locale.US,
            "%.2f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }
}
