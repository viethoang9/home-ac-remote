package com.mhi.acremote

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RemoteViewModel(
    private val tx: Transmitter,
    var use152: Boolean = false
) : ViewModel() {
    private val _state = MutableStateFlow(AcState())
    val state: StateFlow<AcState> = _state

    fun send(next: AcState): Result<Unit> {
        _state.value = next
        val freq = Mhi88Encoder.FREQ
        val pattern = if (use152) Mhi152Encoder.toPattern(next) else Mhi88Encoder.toPattern(next)
        return tx.transmit(freq, pattern)
    }
    fun togglePower() = send(_state.value.copy(power = !_state.value.power))
    fun setTemp(t: Int) = send(_state.value.copy(temp = t.coerceIn(18, 30)))
    fun setMode(m: Mode) = send(_state.value.copy(mode = m))
    fun cycleFan() {
        val order = listOf(Fan.AUTO, Fan.LOW, Fan.MED, Fan.HIGH, Fan.TURBO, Fan.ECONO)
        val i = (order.indexOf(_state.value.fan) + 1) % order.size
        send(_state.value.copy(fan = order[i]))
    }
}
