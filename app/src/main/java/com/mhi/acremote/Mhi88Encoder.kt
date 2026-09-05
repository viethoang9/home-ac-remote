package com.mhi.acremote

object Mhi88Encoder {
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
        Fan.AUTO -> 0; Fan.LOW -> 2; Fan.MED -> 3
        Fan.HIGH -> 4; Fan.TURBO -> 6; Fan.ECONO -> 7
    }

    fun swingVBits(v: SwingV): Int = when (v) {
        SwingV.OFF -> 0; SwingV.HIGH -> 1; SwingV.MIDDLE -> 3
        SwingV.SWING -> 4; SwingV.LOW -> 5; SwingV.HIGHEST -> 6
        SwingV.LOWEST -> 7
    }

    fun swingHBits(h: SwingH, threeD: Boolean): Int {
        if (threeD) return 14
        return when (h) {
            SwingH.OFF -> 0; SwingH.LEFTMAX -> 1; SwingH.RIGHTMAX -> 2
            SwingH.LEFT -> 5; SwingH.MIDDLE -> 9; SwingH.RIGHT -> 13
            SwingH.SWING -> 8; SwingH.LEFTRIGHT -> 6; SwingH.RIGHTLEFT -> 10
        }
    }

    fun buildBytes(s: AcState): ByteArray {
        val b = ByteArray(11)
        b[0] = 0xAD.toByte()
        b[1] = 0x51
        b[2] = 0x3C
        b[3] = 0xD9.toByte()
        val sv = swingVBits(s.swingV)
        val sh = swingHBits(s.swingH, s.threeD)
        var b5 = 0
        b5 = b5 or ((sv and 0x01) shl 1)
        b5 = b5 or ((sh and 0x03) shl 2)
        if (s.clean) b5 = b5 or 0x20
        b5 = b5 or (((sh ushr 2) and 0x03) shl 6)
        b[5] = b5.toByte()
        var b7 = 0
        b7 = b7 or (((sv ushr 1) and 0x03) shl 3)
        b7 = b7 or ((fanBits(s.fan) and 0x07) shl 5)
        b[7] = b7.toByte()
        var b9 = 0
        b9 = b9 or (modeBits(s.mode) and 0x07)
        if (s.power) b9 = b9 or 0x08
        b9 = b9 or (((s.coercedTemp() - 17) and 0x0F) shl 4)
        b[9] = b9.toByte()
        b[4] = (b[3].toInt() xor 0xFF).toByte()
        b[6] = (b[5].toInt() xor 0xFF).toByte()
        b[8] = (b[7].toInt() xor 0xFF).toByte()
        b[10] = (b[9].toInt() xor 0xFF).toByte()
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
