package com.mhi.acremote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mhi152EncoderTest {
    @Test
    fun heat24_high_matches_reference_vector() {
        val bytes = Mhi152Encoder.buildBytes(
            AcState(power = true, mode = Mode.HEAT, temp = 24, fan = Fan.HIGH))
        val expected = byteArrayOf(
            0xAD.toByte(), 0x51, 0x3C, 0xE5.toByte(), 0x1A,
            0x0C, 0xF3.toByte(), 0x07, 0xF8.toByte(), 0x03, 0xFC.toByte(),
            0x00, 0xFF.toByte(), 0x00, 0xFF.toByte(), 0x00, 0xFF.toByte(),
            0x80.toByte(), 0x7F)
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun inverted_byte_pairs_hold() {
        val s = AcState(power = true, mode = Mode.COOL, temp = 22, fan = Fan.LOW,
            clean = true, sleep = true, threeD = true)
        val b = Mhi152Encoder.buildBytes(s)
        var i = 3
        while (i < 18) {
            assertEquals("pair at $i", (b[i].toInt() and 0xFF) xor 0xFF, b[i + 1].toInt() and 0xFF)
            i += 2
        }
    }

    @Test
    fun pattern_uses_reference_timings_and_repeats_under_2s() {
        val pattern = Mhi152Encoder.toPattern(AcState(power = true))
        assertEquals(3140, pattern[0])
        assertEquals(1630, pattern[1])
        assertTrue(pattern.sum() < 2000000)
    }
}
