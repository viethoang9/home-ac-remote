package com.mhi.acremote

object Mhi152Encoder {
    const val FREQ = 38000
    const val HDR_MARK = 3140
    const val HDR_SPACE = 1630
    const val BIT_MARK = 370
    const val ONE_SPACE = 420
    const val ZERO_SPACE = 1220
    const val GAP = 30000
    const val REPEAT = 2

    fun modeBits(m: Mode): Int = when (m) {
        Mode.AUTO -> 0; Mode.COOL -> 1; Mode.DRY -> 2
        Mode.FAN -> 3; Mode.HEAT -> 4
    }

    fun fanBits(f: Fan): Int = when (f) {
        Fan.AUTO -> 0; Fan.LOW -> 1; Fan.MED -> 2
        Fan.HIGH -> 3; Fan.TURBO -> 8; Fan.ECONO -> 6
    }

    fun swingVBits(v: SwingV): Int = when (v) {
        SwingV.SWING -> 0; SwingV.HIGHEST -> 1; SwingV.HIGH -> 2
        SwingV.MIDDLE -> 3; SwingV.LOW -> 4; SwingV.LOWEST -> 5
        SwingV.OFF -> 6
    }

    fun swingHBits(h: SwingH): Int = when (h) {
        SwingH.SWING -> 0; SwingH.LEFTMAX -> 1; SwingH.LEFT -> 2
        SwingH.MIDDLE -> 3; SwingH.RIGHT -> 4; SwingH.RIGHTMAX -> 5
        SwingH.RIGHTLEFT -> 6; SwingH.LEFTRIGHT -> 7; SwingH.OFF -> 8
    }

    fun buildBytes(s: AcState): ByteArray {
        val b = ByteArray(19)
        b[0] = 0xAD.toByte()
        b[1] = 0x51
        b[2] = 0x3C
        b[3] = 0xE5.toByte()
        var b5 = 0
        b5 = b5 or (modeBits(s.mode) and 0x07)
        if (s.power) b5 = b5 or 0x08
        if (s.clean) {
            b5 = b5 or 0x20
            b5 = b5 or 0x40
        }
        b[5] = b5.toByte()
        b[7] = ((s.coercedTemp() - 17) and 0x0F).toByte()
        b[9] = (fanBits(s.fan) and 0x0F).toByte()
        var b11 = 0
        if (s.threeD) {
            b11 = b11 or 0x02
            b11 = b11 or 0x10
        }
        b11 = b11 or ((swingVBits(s.swingV) and 0x07) shl 5)
        b[11] = b11.toByte()
        b[13] = (swingHBits(s.swingH) and 0x0F).toByte()
        var b15 = 0
        if (s.sleep) b15 = b15 or 0x40
        b[15] = b15.toByte()
        b[17] = 0x80.toByte()
        var i = 3
        while (i < 18) {
            b[i + 1] = (b[i].toInt() xor 0xFF).toByte()
            i += 2
        }
        return b
    }

    fun toPattern(s: AcState): IntArray {
        val bytes = buildBytes(s)
        val out = mutableListOf(HDR_MARK, HDR_SPACE)
        repeat(REPEAT) { r ->
            for (by in bytes) {
                for (i in 0..7) {
                    val bit = (by.toInt() shr i) and 1
                    out.add(BIT_MARK)
                    out.add(if (bit == 1) ONE_SPACE else ZERO_SPACE)
                }
            }
            out.add(BIT_MARK)
            if (r < REPEAT - 1) out.add(GAP)
        }
        return out.toIntArray()
    }
}
