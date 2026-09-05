package com.mhi.acremote

import android.hardware.ConsumerIrManager

interface Transmitter {
    fun transmit(freq: Int, pattern: IntArray): Result<Unit>
}

class AndroidIrTransmitter(private val mgr: ConsumerIrManager) : Transmitter {
    override fun transmit(freq: Int, pattern: IntArray): Result<Unit> = runCatching {
        if (!mgr.hasIrEmitter()) throw IllegalStateException("No IR emitter")
        var attempt = 0
        while (true) {
            try {
                mgr.transmit(freq, pattern)
                return@runCatching
            } catch (e: Exception) {
                if (++attempt >= 2) throw e
            }
        }
    }
}

class FakeIrTransmitter : Transmitter {
    var lastFreq: Int = 0
    var lastPattern: IntArray = intArrayOf()
    override fun transmit(freq: Int, pattern: IntArray): Result<Unit> {
        lastFreq = freq
        lastPattern = pattern
        return Result.success(Unit)
    }
}
