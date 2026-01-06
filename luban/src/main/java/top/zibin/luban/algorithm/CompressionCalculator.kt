package top.zibin.luban.algorithm

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class CompressionTarget(
    val width: Int,
    val height: Int,
    val estimatedSizeKb: Int,
    val isLongImage: Boolean = false,
    val targetSizeKb: Int? = null
)

class CompressionCalculator {
    private val baseShort = 1440
    private val wallLong = 10800
    private val wallRatio = 0.4
    private val trapPixels = 40_960_000L
    private val capPixels = 10_240_000L

    fun calculateTarget(width: Int, height: Int): CompressionTarget {
        if (width <= 0 || height <= 0) {
            return CompressionTarget(0, 0, 0)
        }

        val shortSide = min(width, height)
        val longSide = max(width, height)
        val ratio = shortSide.toDouble() / longSide
        val pixelCount = width.toLong() * height

        var targetShort = baseShort
        var targetLong = (targetShort / ratio).toInt()

        if (longSide >= wallLong && ratio > wallRatio) {
            targetLong = baseShort
            targetShort = (targetLong * ratio).toInt()
        }

        if (pixelCount > trapPixels) {
            val trapShort = (shortSide * 0.25).toInt()
            if (trapShort < targetShort) {
                targetShort = trapShort
                targetLong = (targetShort / ratio).toInt()
            }
        }

        if (targetShort > shortSide) {
            targetShort = shortSide
            targetLong = longSide
        }

        var currentPixels = targetShort.toLong() * targetLong
        if (currentPixels > capPixels) {
            val scale = floor(sqrt(capPixels.toDouble() / currentPixels) * 1000) / 1000.0
            targetShort = (targetShort * scale).toInt()
            targetLong = (targetLong * scale).toInt()
        }

        targetShort = (targetShort / 2) * 2
        targetLong = (targetLong / 2) * 2
        
        targetShort = maxOf(2, targetShort)
        targetLong = maxOf(2, targetLong)

        val (finalW, finalH) = if (width < height) {
            Pair(targetShort, targetLong)
        } else {
            Pair(targetLong, targetShort)
        }

        val finalPixels = finalW.toLong() * finalH
        
        var factor = when {
            finalPixels < 500_000L -> 0.0005
            finalPixels < 1_000_000L -> 0.00015
            finalPixels < 3_000_000L -> 0.00011
            else -> 0.000025
        }
        
        var estimatedSize = (finalPixels * factor).toInt()
        estimatedSize = maxOf(20, estimatedSize)
        
        if (ratio < 0.2 && estimatedSize < 400) {
            estimatedSize = maxOf(estimatedSize, 250)
        }
        
        val isLongImage = ratio <= 0.5
        val targetSizeKb = if (isLongImage) estimatedSize else null

        return CompressionTarget(
            finalW,
            finalH,
            estimatedSize,
            isLongImage,
            targetSizeKb
        )
    }
}
