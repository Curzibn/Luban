package top.zibin.luban.api

import android.content.Context
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

/**
 * 面向 Java 用户或偏好构建器模式 / 回调式 API 的兼容层。
 *
 * 该类封装了基于协程实现的 [Luban] 压缩 API。
 *
 * Compatibility layer for Java users or those who prefer a Builder pattern / Callback based API.
 *
 * This class wraps the coroutine-based [Luban] API.
 */
class LubanCompat private constructor(builder: Builder) {
    private val context: Context? = builder.context
    private val targetDir: File? = builder.targetDir
    private val compressListener: OnCompressListener? = builder.compressListener
    private val inputs: List<Any> = builder.inputs
    private val lifecycleOwner: LifecycleOwner? = builder.lifecycleOwner
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private fun launch(): CompressionTask {
        if (inputs.isEmpty() || compressListener == null) {
            return object : CompressionTask { override fun cancel() {} }
        }

        lifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    cancel()
                }
            }
        })

        compressListener.onStart()

        scope.launch {
            for (input in inputs) {
                if (!isActive) break
                
                launch {
                    try {
                        val outputDir = targetDir ?: context?.cacheDir ?: throw IllegalArgumentException("Target directory or Context must be provided")
                        
                        val compressedFileResult = when (input) {
                            is String -> {
                                val file = File(input)
                                if (file.exists()) {
                                    Luban.compress(file, outputDir)
                                } else {
                                    Result.failure(IllegalArgumentException("File not found: $input"))
                                }
                            }
                            is File -> Luban.compress(input, outputDir)
                            is Uri -> {
                                if (context != null) {
                                    Luban.compress(context, input, outputDir)
                                } else {
                                    Result.failure(IllegalArgumentException("Context required for Uri compression"))
                                }
                            }
                            else -> Result.failure(IllegalArgumentException("Unsupported input type"))
                        }
                        
                        val result = compressedFileResult.getOrThrow()
                        if (isActive) {
                            compressListener.onSuccess(result)
                        }
                    } catch (e: Throwable) {
                        if (isActive) {
                            compressListener.onError(e)
                        }
                    }
                }
            }
        }

        return object : CompressionTask {
            override fun cancel() {
                this@LubanCompat.cancel()
            }
        }
    }

    private fun cancel() {
        job.cancel()
    }
    
    private val isActive: Boolean
        get() = job.isActive

    /**
     * 用于构建压缩请求的 Builder。
     *
     * Builder for constructing a compression request.
     *
     * @param context Android 上下文。
     * The Android Context.
     */
    class Builder(internal val context: Context?) {
        internal var targetDir: File? = null
        internal var compressListener: OnCompressListener? = null
        internal val inputs: MutableList<Any> = mutableListOf()
        internal var lifecycleOwner: LifecycleOwner? = null

        /**
         * 向压缩任务中添加单个文件。
         *
         * Adds a file to the compression list.
         *
         * @param file 需要压缩的图片文件。
         * The image file to compress.
         */
        fun load(file: File): Builder {
            inputs.add(file)
            return this
        }

        /**
         * 向压缩任务中添加文件路径。
         *
         * Adds a file path to the compression list.
         *
         * @param path 图片文件的路径。
         * The path to the image file.
         */
        fun load(path: String): Builder {
            inputs.add(path)
            return this
        }

        /**
         * 向压缩任务中添加图片的 Uri。
         *
         * Adds a Uri to the compression list.
         *
         * @param uri 图片的 Uri。
         * The image Uri.
         */
        fun load(uri: Uri): Builder {
            inputs.add(uri)
            return this
        }
        
        /**
         * 批量添加待压缩的图片（支持 File、路径字符串或 Uri）。
         *
         * Adds a list of items (File, String path, or Uri) to the compression list.
         *
         * @param list 待压缩图片的列表。
         * List of images to compress.
         */
        fun <T> load(list: List<T>): Builder {
            for (item in list) {
                when (item) {
                    is String -> load(item)
                    is File -> load(item)
                    is Uri -> load(item)
                }
            }
            return this
        }

        /**
         * 设置压缩结果输出目录（通过路径字符串）。
         *
         * Sets the target directory for compressed images.
         *
         * @param targetDir 目标目录路径。
         * The directory path.
         */
        fun setTargetDir(targetDir: String): Builder {
            this.targetDir = File(targetDir)
            return this
        }
        
        /**
         * 设置压缩结果输出目录（通过目录 File 对象）。
         *
         * Sets the target directory for compressed images.
         *
         * @param targetDir 目标目录 File。
         * The directory file.
         */
        fun setTargetDir(targetDir: File): Builder {
            this.targetDir = targetDir
            return this
        }

        /**
         * 设置压缩过程的回调监听器。
         *
         * Sets the listener for compression events.
         *
         * @param listener 压缩回调监听器。
         * The callback listener.
         */
        fun setCompressListener(listener: OnCompressListener): Builder {
            this.compressListener = listener
            return this
        }

        /**
         * 将压缩任务绑定到指定的 LifecycleOwner，生命周期销毁时会自动取消任务。
         *
         * Binds the compression task to a LifecycleOwner.
         * The task will be automatically cancelled when the lifecycle is destroyed.
         *
         * @param lifecycleOwner 生命周期拥有者（例如 Activity 或 Fragment）。
         * The lifecycle owner (e.g., Activity or Fragment).
         */
        fun bindLifecycle(lifecycleOwner: LifecycleOwner): Builder {
            this.lifecycleOwner = lifecycleOwner
            return this
        }

        /**
         * 启动压缩任务。
         * 
         * Starts the compression task.
         * 
         * @return 可用于取消操作的 [CompressionTask]。
         * A [CompressionTask] that can be used to cancel the operation.
         */
        fun launch(): CompressionTask {
            return LubanCompat(this).launch()
        }
    }
    
    companion object {
        /**
         * 创建一个新的 Builder 实例。
         *
         * Creates a new Builder instance.
         *
         * @param context Android 上下文。
         * The Android Context.
         */
        @JvmStatic
        fun with(context: Context): Builder {
            return Builder(context)
        }
    }
}
