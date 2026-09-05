package com.mhi.acremote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.store by preferencesDataStore("mhi_remote")

class PresetStore(private val ctx: Context) {
    private val k1 = stringPreferencesKey("preset1")
    private val k2 = stringPreferencesKey("preset2")
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
