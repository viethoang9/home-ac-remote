package com.mhi.acremote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.store by preferencesDataStore("mhi_remote")

fun encodeFull(s: AcState, use152: Boolean): String = listOf(
    s.power.toString(), s.mode.name, s.temp.toString(), s.fan.name,
    s.swingV.name, s.swingH.name, s.threeD.toString(), s.sleep.toString(),
    s.clean.toString(), use152.toString()).joinToString(",")

fun decodeFull(v: String?): Pair<AcState, Boolean>? {
    if (v == null) return null
    return try {
        val p = v.split(",")
        if (p.size != 10) return null
        AcState(
            power = p[0].toBooleanStrict(), mode = Mode.valueOf(p[1]),
            temp = p[2].toInt().coerceIn(18, 30), fan = Fan.valueOf(p[3]),
            swingV = SwingV.valueOf(p[4]), swingH = SwingH.valueOf(p[5]),
            threeD = p[6].toBooleanStrict(), sleep = p[7].toBooleanStrict(),
            clean = p[8].toBooleanStrict()) to p[9].toBooleanStrict()
    } catch (e: Exception) {
        null
    }
}

class PresetStore(private val ctx: Context) {
    private val k1 = stringPreferencesKey("preset1")
    private val k2 = stringPreferencesKey("preset2")
    private val lastKey = stringPreferencesKey("last_state")

    suspend fun saveLast(s: AcState, use152: Boolean) {
        ctx.store.edit { it[lastKey] = encodeFull(s, use152) }
    }

    suspend fun loadLast(): Pair<AcState, Boolean>? {
        return ctx.store.data.map { decodeFull(it[lastKey]) }.first()
    }

    private fun encode(s: AcState) = "${s.power},${s.mode},${s.temp},${s.fan}"
    private fun decode(v: String?): AcState? {
        if (v == null) return null
        val p = v.split(",")
        return AcState(power = p[0].toBoolean(), mode = Mode.valueOf(p[1]), temp = p[2].toInt(), fan = Fan.valueOf(p[3]))
    }
    suspend fun save(slot: Int, s: AcState) {
        ctx.store.edit { it[if (slot == 1) k1 else k2] = encode(s) }
    }
    suspend fun load(slot: Int): AcState? {
        return ctx.store.data.map { decode(it[if (slot == 1) k1 else k2]) }.first()
    }
}
