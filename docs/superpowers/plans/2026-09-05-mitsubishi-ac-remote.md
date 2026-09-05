# Mitsubishi AC Remote Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build offline Kotlin Android app that controls MHI SRK12CT-S5 via phone IR blaster with Classic Replica UI.

**Architecture:** Single-activity Compose app. AcState -> Mhi88Encoder (11-byte frame, 38kHz timings) -> IrTransmitter wrapping ConsumerIrManager.transmit. DataStore persists state. Mhi152Encoder behind debug flag for CT variant fallback.

**Tech Stack:** Kotlin 1.9, Jetpack Compose BOM 2024.02, Material3, minSdk 24 targetSdk 34, JUnit4, DataStore Preferences 1.0, ConsumerIrManager API 19+

---

## File Structure

- `settings.gradle` — includes `:app`, plugin repos
- `build.gradle` — top-level plugins
- `app/build.gradle` — compose, datastore, test deps, minSdk 24
- `app/src/main/AndroidManifest.xml` — CONSUMER_IR permission + feature, MainActivity entry
- `app/src/main/java/com/mhi/acremote/AcState.kt` — enums Mode/Fan/SwingV/SwingH + data class + clamp
- `app/src/main/java/com/mhi/acremote/Mhi88Encoder.kt` — sig AD-51-3C-D9-26, buildBytes + toPattern
- `app/src/main/java/com/mhi/acremote/Mhi152Encoder.kt` — fallback 19-byte frame, sig AD-51-3C-E5-1A
- `app/src/main/java/com/mhi/acremote/IrTransmitter.kt` — interface + Android impl + Fake for demo/test
- `app/src/main/java/com/mhi/acremote/RemoteViewModel.kt` — holds AcState StateFlow, calls transmit
- `app/src/main/java/com/mhi/acremote/RemoteScreen.kt` — Classic Replica LCD + button grid
- `app/src/main/java/com/mhi/acremote/MainActivity.kt` — hasIrEmitter check, demo banner, setContent
- `app/src/main/java/com/mhi/acremote/PresetStore.kt` — DataStore 2 slots
- `app/src/test/java/com/mhi/acremote/AcStateTest.kt` — clamp tests
- `app/src/test/java/com/mhi/acremote/Mhi88EncoderTest.kt` — vectors, checksum, timing length

---

### Task 1: Project Scaffold

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `app/build.gradle`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write the failing test**

No unit test for scaffold. Verify file absence instead:

```bash
test -f settings.gradle && echo EXISTS || echo MISSING
```

- [ ] **Step 2: Run test to verify it fails**

Run: `test -f settings.gradle && echo EXISTS || echo MISSING`
Expected: `MISSING`

- [ ] **Step 3: Write minimal implementation**

`settings.gradle`:
```gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MhiAcRemote"
include(":app")
```

`build.gradle`:
```gradle
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
```

`app/build.gradle`:
```gradle
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.mhi.acremote"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.mhi.acremote"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    testImplementation("junit:junit:4.13.2")
}
```

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.TRANSMIT_IR" />
    <uses-feature android:name="android.hardware.consumerir" android:required="false" />
    <application android:label="MHI Remote" android:theme="@android:style/Theme.Material.Light">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `test -f settings.gradle && echo EXISTS || echo MISSING`
Expected: `EXISTS`

- [ ] **Step 5: Commit**

```bash
git add settings.gradle build.gradle app/build.gradle app/src/main/AndroidManifest.xml
git commit -m "feat: add android scaffold with IR permission"
```

---

### Task 2: AcState Model

**Files:**
- Create: `app/src/main/java/com/mhi/acremote/AcState.kt`
- Test: `app/src/test/java/com/mhi/acremote/AcStateTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/mhi/acremote/AcStateTest.kt`:
```kotlin
package com.mhi.acremote

import org.junit.Assert.assertEquals
import org.junit.Test

class AcStateTest {
    @Test
    fun temp_clampsTo18_30() {
        assertEquals(18, AcState(power = true, mode = Mode.COOL, temp = 10, fan = Fan.AUTO).coercedTemp())
        assertEquals(30, AcState(power = true, mode = Mode.COOL, temp = 35, fan = Fan.AUTO).coercedTemp())
        assertEquals(24, AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO).coercedTemp())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mhi.acremote.AcStateTest" 2>&1 | tail -n 20`
Expected: FAIL with "unresolved reference: AcState"

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/mhi/acremote/AcState.kt`:
```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mhi.acremote.AcStateTest" 2>&1 | tail -n 20`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mhi/acremote/AcState.kt app/src/test/java/com/mhi/acremote/AcStateTest.kt
git commit -m "feat: add AcState with temp clamp"
```

---

### Task 3: Mhi88Encoder

**Files:**
- Create: `app/src/main/java/com/mhi/acremote/Mhi88Encoder.kt`
- Test: `app/src/test/java/com/mhi/acremote/Mhi88EncoderTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/mhi/acremote/Mhi88EncoderTest.kt`:
```kotlin
package com.mhi.acremote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mhi88EncoderTest {
    @Test
    fun signature_bytes_match() {
        val bytes = Mhi88Encoder.buildBytes(AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        assertArrayEquals(byteArrayOf(0xAD.toByte(), 0x51, 0x3C, 0xD9.toByte(), 0x26), bytes.copyOfRange(0, 5))
    }
    @Test
    fun pattern_startsWithHeader_andUnder2Sec() {
        val pattern = Mhi88Encoder.toPattern(AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        assertTrue(pattern[0] == 3200)
        assertTrue(pattern[1] == 1600)
        assertTrue(pattern.sum() < 2000000)
    }
    @Test
    fun powerOff_setsPowerBit() {
        val on = Mhi88Encoder.buildBytes(AcState(power = true, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        val off = Mhi88Encoder.buildBytes(AcState(power = false, mode = Mode.COOL, temp = 24, fan = Fan.AUTO))
        assertTrue(on[9].toInt() and 0x08 == 0)
        assertTrue(off[9].toInt() and 0x08 != 0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mhi.acremote.Mhi88EncoderTest" 2>&1 | tail -n 20`
Expected: FAIL with "unresolved reference: Mhi88Encoder"

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/mhi/acremote/Mhi88Encoder.kt`:
```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mhi.acremote.Mhi88EncoderTest" 2>&1 | tail -n 20`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mhi/acremote/Mhi88Encoder.kt app/src/test/java/com/mhi/acremote/Mhi88EncoderTest.kt
git commit -m "feat: add MHI 88-bit encoder with header and checksum"
```

---

### Task 4: IrTransmitter + ViewModel

**Files:**
- Create: `app/src/main/java/com/mhi/acremote/IrTransmitter.kt`
- Create: `app/src/main/java/com/mhi/acremote/RemoteViewModel.kt`
- Create: `app/src/main/java/com/mhi/acremote/Mhi152Encoder.kt`

- [ ] **Step 1: Write the failing test**

Add to `Mhi88EncoderTest.kt` new case in same file edit, or create `IrTransmitterTest.kt`:
```kotlin
package com.mhi.acremote

import org.junit.Assert.assertTrue
import org.junit.Test

class IrTransmitterTest {
    @Test
    fun fake_transmits_without_crash() {
        val fake = FakeIrTransmitter()
        val r = fake.transmit(Mhi88Encoder.FREQ, Mhi88Encoder.toPattern(AcState(power = true)))
        assertTrue(r.isSuccess)
        assertTrue(fake.lastFreq == 38000)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mhi.acremote.IrTransmitterTest" 2>&1 | tail -n 20`
Expected: FAIL with "unresolved reference: FakeIrTransmitter"

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/mhi/acremote/IrTransmitter.kt`:
```kotlin
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
```

`app/src/main/java/com/mhi/acremote/Mhi152Encoder.kt`:
```kotlin
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
```

`app/src/main/java/com/mhi/acremote/RemoteViewModel.kt`:
```kotlin
package com.mhi.acremote

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mhi.acremote.IrTransmitterTest" 2>&1 | tail -n 20`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mhi/acremote/IrTransmitter.kt app/src/main/java/com/mhi/acremote/RemoteViewModel.kt app/src/main/java/com/mhi/acremote/Mhi152Encoder.kt
git commit -m "feat: add IR transmitter with retry and viewmodel"
```

---

### Task 5: Classic Replica UI

**Files:**
- Create: `app/src/main/java/com/mhi/acremote/RemoteScreen.kt`
- Create: `app/src/main/java/com/mhi/acremote/MainActivity.kt`
- Create: `app/src/main/java/com/mhi/acremote/PresetStore.kt`

- [ ] **Step 1: Write the failing test**

Manual UI check — verify file missing:
```bash
test -f app/src/main/java/com/mhi/acremote/RemoteScreen.kt && echo EXISTS || echo MISSING
```

- [ ] **Step 2: Run test to verify it fails**

Run: `test -f app/src/main/java/com/mhi/acremote/RemoteScreen.kt && echo EXISTS || echo MISSING`
Expected: `MISSING`

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/mhi/acremote/PresetStore.kt`:
```kotlin
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
```

`app/src/main/java/com/mhi/acremote/RemoteScreen.kt`:
```kotlin
package com.mhi.acremote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RemoteScreen(vm: RemoteViewModel) {
    val s by vm.state.collectAsState()
    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (s.power) "${s.mode} ${s.coercedTemp()}C ${s.fan}" else "OFF", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { vm.togglePower() }) { Text(if (s.power) "Power OFF" else "Power ON") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.setTemp(s.temp - 1) }) { Text("-") }
            Text("${s.coercedTemp()}C", Modifier.padding(horizontal = 16.dp))
            Button(onClick = { vm.setTemp(s.temp + 1) }) { Text("+") }
        }
        Row { Mode.values().forEach { m -> Button(onClick = { vm.setMode(m) }, Modifier.padding(2.dp)) { Text(m.name.take(4)) } } }
        Row {
            Button(onClick = { vm.cycleFan() }) { Text("Fan:${s.fan.name}") }
            Button(onClick = { vm.send(s.copy(swingV = if (s.swingV == SwingV.SWING) SwingV.OFF else SwingV.SWING)) }) { Text("SwingV") }
            Button(onClick = { vm.send(s.copy(threeD = !s.threeD)) }) { Text(if (s.threeD) "3D:ON" else "3D:OFF") }
        }
        Row {
            Button(onClick = { vm.send(s.copy(sleep = !s.sleep)) }) { Text("Sleep") }
            Button(onClick = { vm.send(s.copy(clean = !s.clean)) }) { Text("Clean") }
            Button(onClick = { vm.send(s.copy(swingH = if (s.swingH == SwingH.SWING) SwingH.OFF else SwingH.SWING)) }) { Text("SwingH") }
        }
        Row {
            Button(onClick = { vm.send(s.copy(temp = s.temp)) }) { Text("Timer") }
            Button(onClick = { vm.use152 = !vm.use152 }) { Text(if (vm.use152) "152:ON" else "152:OFF") }
        }
    }
}
```

`app/src/main/java/com/mhi/acremote/MainActivity.kt`:
```kotlin
package com.mhi.acremote

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mgr = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        val hasIr = mgr?.hasIrEmitter() == true
        val tx: Transmitter = if (hasIr && mgr != null) AndroidIrTransmitter(mgr) else FakeIrTransmitter()
        val vm = RemoteViewModel(tx)
        setContent {
            MaterialTheme {
                if (!hasIr) Text("No IR emitter — Demo mode")
                RemoteScreen(vm)
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:assembleDebug 2>&1 | tail -n 20`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mhi/acremote/RemoteScreen.kt app/src/main/java/com/mhi/acremote/MainActivity.kt app/src/main/java/com/mhi/acremote/PresetStore.kt
git commit -m "feat: add classic replica UI with demo fallback"
```

---

### Task 6: On-device Verification

**Files:**
- Modify: `app/src/main/java/com/mhi/acremote/RemoteViewModel.kt` — no code change, manual checklist

- [ ] **Step 1: Write the failing test**

Checklist file does not exist:
```bash
test -f MANUAL_CHECKLIST.md && echo EXISTS || echo MISSING
```

- [ ] **Step 2: Run test to verify it fails**

Run: `test -f MANUAL_CHECKLIST.md && echo EXISTS || echo MISSING`
Expected: `MISSING`

- [ ] **Step 3: Write minimal implementation**

Create `MANUAL_CHECKLIST.md`:
```markdown
# Manual Checklist — Xiaomi IR + SRK12CT-S5, 3m line-of-sight
- [ ] Power ON beeps, louver opens
- [ ] Cool 24 Auto works
- [ ] Temp 18 and 30 limits beep
- [ ] Heat, Dry, Fan, Auto each beep
- [ ] Fan Low/Med/High/Turbo/Econo each beep
- [ ] SwingV toggle moves louver
- [ ] 3D toggle beeps
- [ ] Sleep + Clean beep
- [ ] Power OFF beeps, louver closes
- [ ] If no beep: Settings toggle use152=true and retry Power ON
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest 2>&1 | tail -n 5`
Expected: `BUILD SUCCESSFUL` — all unit tests pass

- [ ] **Step 5: Commit**

```bash
git add MANUAL_CHECKLIST.md
git commit -m "docs: add manual IR verification checklist"
```
