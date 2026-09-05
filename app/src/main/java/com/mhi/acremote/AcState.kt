package com.mhi.acremote

enum class Mode { AUTO, COOL, DRY, FAN, HEAT }
enum class Fan { AUTO, LOW, MED, HIGH, TURBO, ECONO }
enum class SwingV { SWING, HIGHEST, HIGH, MIDDLE, LOW, LOWEST, OFF }
enum class SwingH { SWING, LEFTMAX, LEFT, MIDDLE, RIGHT, RIGHTMAX, LEFTRIGHT, RIGHTLEFT, OFF }

data class AcState(
    val power: Boolean = false,
    val mode: Mode = Mode.COOL,
    val temp: Int = 24,
    val fan: Fan = Fan.AUTO,
    val swingV: SwingV = SwingV.SWING,
    val swingH: SwingH = SwingH.SWING,
    val threeD: Boolean = false,
    val sleep: Boolean = false,
    val clean: Boolean = false
) {
    fun coercedTemp(): Int = temp.coerceIn(18, 30)
}
