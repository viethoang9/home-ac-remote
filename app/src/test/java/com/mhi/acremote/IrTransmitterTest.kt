package com.mhi.acremote

import org.junit.Assert.assertTrue
import org.junit.Test

class IrTransmitterTest {
    @Test
    fun fake_transmits_without_crash() {
        val fake = FakeIrTransmitter()
        val r = fake.transmit(Mhi88Encoder.FREQ, Mhi88Encoder.toPattern(AcState(power = true)))
        assertTrue(r.isSuccess)
        assertTrue(fake.lastFreq == 38000)
    }
}
