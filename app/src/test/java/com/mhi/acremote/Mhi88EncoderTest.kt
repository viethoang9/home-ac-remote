package com.mhi.acremote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mhi88EncoderTest {
    @Test
    fun signature_bytes_match() {
        val bytes = Mhi88Encoder.buildBytes(AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        assertArrayEquals(byteArrayOf(0xAD.toByte(), 0x51, 0x3C, 0xD9.toByte(), 0x26), bytes.copyOfRange(0, 5))
    }

    @Test
    fun cool24_frame_matches_hand_derived_vector() {
        val bytes = Mhi88Encoder.buildBytes(
            AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO,
                swingV = SwingV.OFF, swingH = SwingH.OFF))
        val expected = byteArrayOf(
            0xAD.toByte(), 0x51, 0x3C, 0xD9.toByte(), 0x26,
            0x00, 0xFF.toByte(), 0x00, 0xFF.toByte(), 0x79, 0x86.toByte())
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun real_capture_vector_dry25_leftright() {
        val bytes = Mhi88Encoder.buildBytes(
            AcState(power = true, mode = Mode.DRY, temp = 25, fan = Fan.AUTO,
                swingV = SwingV.OFF, swingH = SwingH.LEFTRIGHT))
        val expected = byteArrayOf(
            0xAD.toByte(), 0x51, 0x3C, 0xD9.toByte(), 0x26,
            0x48, 0xB7.toByte(), 0x00, 0xFF.toByte(), 0x8A.toByte(), 0x75)
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun inverted_byte_pairs_hold() {
        val states = listOf(
            AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO),
            AcState(power = false, mode = Mode.HEAT, temp = 30, fan = Fan.TURBO,
                swingV = SwingV.HIGH, swingH = SwingH.LEFT, clean = true),
            AcState(power = true, mode = Mode.FAN, temp = 18, fan = Fan.ECONO, threeD = true))
        for (s in states) {
            val b = Mhi88Encoder.buildBytes(s)
            for (i in listOf(3, 5, 7, 9)) {
                assertEquals("pair at $i", (b[i].toInt() and 0xFF) xor 0xFF, b[i + 1].toInt() and 0xFF)
            }
        }
    }

    @Test
    fun power_bit_set_when_on() {
        val on = Mhi88Encoder.buildBytes(AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        val off = Mhi88Encoder.buildBytes(AcState(power = false, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        assertTrue(on[9].toInt() and 0x08 != 0)
        assertTrue(off[9].toInt() and 0x08 == 0)
    }

    @Test
    fun pattern_uses_reference_timings_and_repeats_under_2s() {
        val pattern = Mhi88Encoder.toPattern(AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        assertEquals(3140, pattern[0])
        assertEquals(1630, pattern[1])
        assertEquals(370, pattern[2])
        assertTrue(pattern.contains(420))
        assertTrue(pattern.contains(1220))
        assertTrue(pattern.sum() < 2000000)
    }
}
