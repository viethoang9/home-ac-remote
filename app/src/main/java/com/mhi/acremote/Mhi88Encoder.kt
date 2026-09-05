package com.mhi.acremote

object Mhi88Encoder {
    const val FREQ = 38000
    private val SIG = byteArrayOf(0xAD.toByte(), 0x51, 0x3C, 0xD9.toByte(), 0x26)

    fun buildBytes(s: AcState): ByteArray {
        val b = ByteArray(11)
        SIG.copyInto(b, 0)
        b[5] = 0x04
        b[6] = 0xFA.toByte()
        var fanBits = when (s.fan) {
            Fan.AUTO -> 0xE0; Fan.LOW -> 0xA0; Fan.MED -> 0x80
            Fan.HIGH -> 0x60; Fan.TURBO -> 0x20; Fan.ECONO -> 0x00
        }
        if (s.swingV == SwingV.SWING) fanBits = fanBits or 0x0A
        b[7] = fanBits.toByte()
        b[8] = 0x05
        val modeBits = when (s.mode) {
            Mode.AUTO -> 0x07; Mode.HEAT -> 0x03; Mode.COOL -> 0x06
            Mode.DRY -> 0x05; Mode.FAN -> 0x04
        }
        val powerBit = if (s.power) 0x00 else 0x08
        val tempNibble = ((s.coercedTemp() - 17) and 0x0F)
        val inv = tempNibble xor 0x0F
        b[9] = ((inv shl 4) or modeBits or powerBit).toByte()
        var sum = 0
        for (i in 0..9) sum += b[i].toInt() and 0xFF
        b[10] = (sum and 0xFF).toByte()
        return b
    }

    fun toPattern(s: AcState): IntArray {
        val bytes = buildBytes(s)
        val out = mutableListOf(3200, 1600)
        for (by in bytes) {
            for (i in 0..7) {
                val bit = (by.toInt() shr i) and 1
                out.add(400)
                out.add(if (bit == 1) 1200 else 430)
            }
        }
        out.add(400)
        return out.toIntArray()
    }
}
