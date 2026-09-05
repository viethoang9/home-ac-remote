package com.mhi.acremote

import org.junit.Assert.assertEquals
import org.junit.Test

class AcStateTest {
    @Test
    fun temp_clampsTo18_30() {
        assertEquals(18, AcState(power = true, mode = Mode.COOL, temp = 10, fan = Fan.AUTO).coercedTemp())
        assertEquals(30, AcState(power = true, mode = Mode.COOL, temp = 35, fan = Fan.AUTO).coercedTemp())
        assertEquals(24, AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO).coercedTemp())
    }
}
