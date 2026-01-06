package top.zibin.luban.api

/**
 * 表示一个正在运行的压缩任务。
 *
 * 调用方可以通过该接口取消压缩操作。
 *
 * Represents a running compression task.
 *
 * Allows the caller to cancel the compression operation.
 */
interface CompressionTask {
    /**
     * 取消当前压缩任务。
     *
     * Cancels the compression task.
     */
    fun cancel()
}
