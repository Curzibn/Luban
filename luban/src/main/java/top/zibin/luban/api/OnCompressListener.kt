package top.zibin.luban.api

import java.io.File

interface OnCompressListener {
    fun onStart()
    fun onSuccess(file: File)
    fun onError(e: Throwable)
}
