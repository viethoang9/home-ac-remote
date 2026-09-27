package com.mhi.acremote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewModelPersistTest {
    @Test
    fun send_invokes_onPersist_with_state_and_flag() {
        val vm = RemoteViewModel(FakeIrTransmitter())
        var saved: Pair<AcState, Boolean>? = null
        vm.onPersist = { s, u -> saved = s to u }
        vm.use152 = true
        vm.setTemp(26)
        assertEquals(26, saved?.first?.temp)
        assertEquals(true, saved?.second)
    }

    @Test
    fun restore_sets_state_without_transmitting() {
        val fake = FakeIrTransmitter()
        val vm = RemoteViewModel(fake)
        var persistCalls = 0
        vm.onPersist = { _, _ -> persistCalls++ }
        val s = AcState(power = true, mode = Mode.HEAT, temp = 28, fan = Fan.HIGH)
        vm.restore(s, true)
        assertEquals(s, vm.state.value)
        assertTrue(vm.use152)
        assertEquals(0, persistCalls)
        assertEquals(0, fake.lastPattern.size)
    }
}
