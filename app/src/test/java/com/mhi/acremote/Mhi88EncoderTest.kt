package com.mhi.acremote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mhi88EncoderTest {
    @Test
    fun signature_bytes_match() {
        val bytes = Mhi88Encoder.buildBytes(AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        assertArrayEquals(byteArrayOf(0xAD.toByte(), 0x51, 0x3C, 0xD9.toByte(), 0x26), bytes.copyOfRange(0, 5))
    }
    @Test
    fun pattern_startsWithHeader_andUnder2Sec() {
        val pattern = Mhi88Encoder.toPattern(AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        assertTrue(pattern[0] == 3200)
        assertTrue(pattern[1] == 1600)
        assertTrue(pattern.sum() < 2000000)
    }
    @Test
    fun powerOff_setsPowerBit() {
        val on = Mhi88Encoder.buildBytes(AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        val off = Mhi88Encoder.buildBytes(AcState(power = false, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        assertTrue(on[9].toInt() and 0x08 == 0)
        assertTrue(off[9].toInt() and 0x08 != 0)
    }
}
