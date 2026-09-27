package com.mhi.acremote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StateCodecTest {
    @Test
    fun roundTrip_preservesFullState() {
        val s = AcState(power = true, mode = Mode.HEAT, temp = 28, fan = Fan.TURBO,
            swingV = SwingV.LOW, swingH = SwingH.RIGHT, threeD = true,
            sleep = true, clean = true)
        val (back, use152) = decodeFull(encodeFull(s, true))!!
        assertEquals(s, back)
        assertEquals(true, use152)
    }

    @Test
    fun corrupt_or_short_input_returnsNull() {
        assertNull(decodeFull(null))
        assertNull(decodeFull("true,COOL,24"))
        assertNull(decodeFull("not,a,valid,state,at,all,really,not,ok,fine"))
        assertNull(decodeFull("true,BOGUS,24,AUTO,SWING,SWING,false,false,false,false"))
    }

    @Test
    fun temp_clamped_on_decode() {
        val (back, _) = decodeFull("true,COOL,99,AUTO,SWING,SWING,false,false,false,false")!!
        assertEquals(30, back.temp)
    }
}
