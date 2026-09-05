package com.mhi.acremote

object Mhi152Encoder {
    const val FREQ = 38000
    fun buildBytes(s: AcState): ByteArray {
        val b = ByteArray(19)
        byteArrayOf(0x52, 0xAE.toByte(), 0xC3.toByte(), 0x1A, 0xE5.toByte()).copyInto(b, 0)
        val modeBits = when (s.mode) {
            Mode.AUTO -> 0x07; Mode.HEAT -> 0x03; Mode.COOL -> 0x06
            Mode.DRY -> 0x05; Mode.FAN -> 0x04
        }
        b[5] = ((if (s.power) 0x00 else 0x08) or modeBits).toByte()
        b[7] = ((s.coercedTemp() - 17) and 0x0F).toByte()
        b[9] = when (s.fan) {
            Fan.AUTO -> 0x0F; Fan.LOW -> 0x0E; Fan.MED -> 0x0D
            Fan.HIGH -> 0x0C; Fan.TURBO -> 0x07; Fan.ECONO -> 0x09
        }.toByte()
        b[11] = if (s.swingV == SwingV.SWING) 0xE0.toByte() else 0xC0.toByte()
        return b
    }
    fun toPattern(s: AcState): IntArray {
        val out = mutableListOf(3200, 1600)
        for (by in buildBytes(s)) {
            for (i in 0..7) {
                out.add(400)
                out.add(if (((by.toInt() shr i) and 1) == 1) 1200 else 430)
            }
        }
        out.add(400)
        return out.toIntArray()
    }
}
