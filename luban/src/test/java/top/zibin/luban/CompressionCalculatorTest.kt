package top.zibin.luban

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import top.zibin.luban.algorithm.CompressionCalculator

class CompressionCalculatorTest {

    private val calculator = CompressionCalculator()

    @Test
    fun `calculateTarget returns correct dimensions for standard large images`() {
        val width = 1920
        val height = 1080
        val target = calculator.calculateTarget(width, height)

        assertTrue(target.width <= width)
        assertTrue(target.height <= height)
        assertEquals(0, target.width % 2)
        assertEquals(0, target.height % 2)
    }

    @Test
    fun `calculateTarget handles small images correctly`() {
        val width = 100
        val height = 100
        val target = calculator.calculateTarget(width, height)

        assertEquals(width, target.width)
        assertEquals(height, target.height)
    }

    @Test
    fun `calculateTarget identifies long images`() {
        val width = 1000
        val height = 4000 
        val target = calculator.calculateTarget(width, height)

        assertTrue("Should be identified as long image", target.isLongImage)
    }

    @Test
    fun `full-screen photo ratio 0 point 45 is not a long image`() {
        val target = calculator.calculateTarget(1080, 2400)

        assertFalse("9:20 photo should not be treated as long image", target.isLongImage)
        assertNull(target.targetSizeKb)
    }

    @Test
    fun `eighteen by nine photo ratio 0 point 5 is not a long image`() {
        val target = calculator.calculateTarget(1080, 2160)

        assertFalse("18:9 photo should not be treated as long image", target.isLongImage)
        assertNull(target.targetSizeKb)
    }

    @Test
    fun `ratio 0 point 4 boundary is still a long image`() {
        val target = calculator.calculateTarget(1000, 2500)

        assertTrue("Ratio 0.4 should still be treated as long image", target.isLongImage)
        assertNotNull(target.targetSizeKb)
    }

    @Test
    fun `real long screenshot ratio 0 point 25 is a long image`() {
        val target = calculator.calculateTarget(1000, 4000)

        assertTrue(target.isLongImage)
        assertNotNull(target.targetSizeKb)
    }

    @Test
    fun `calculateTarget handles zero or negative dimensions gracefully`() {
        val target = calculator.calculateTarget(0, 0)
        assertEquals(0, target.width)
        assertEquals(0, target.height)

        val targetNegative = calculator.calculateTarget(-100, 100)
        assertEquals(0, targetNegative.width)
        assertEquals(0, targetNegative.height)
    }
    
    @Test
    fun `calculateTarget respects even dimensions constraint`() {
        val width = 1333
        val height = 750
        val target = calculator.calculateTarget(width, height)
        
        assertEquals(0, target.width % 2)
        assertEquals(0, target.height % 2)
    }
}
