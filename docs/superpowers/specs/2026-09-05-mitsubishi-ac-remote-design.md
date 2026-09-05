# Mitsubishi AC Remote — Android App Design

Date: 2026-09-05
Target: Mitsubishi Heavy Industries SRK12CT-S5, Xiaomi/Redmi IR phone, full replica
Approach: A — Native IR tables, offline. UI: Classic Replica (A)

## 1. Mitsubishi Remote Types Collected

### Mitsubishi Electric (MSZ/MUZ residential + commercial)
- Handheld IR: KM04B, KM04F, KM05B/C, KM06A/B/C/D, KM07K/M, KM08C, KM09E/F, KM15E/F, SG11D, SG13A, SG14D, SG15C/D/F/H, SG20A, SH22A/B/C/D/E/F, SH23C, SH24J/T/H/L/K/M, MS16A, SG162, SG176, KH21A/B
- Wired wall: PAR-21MAA (~$45), PAC-YT52CRA (~$85), PAC-YJ52CRAU (~$120)
- Smart: Kumo Cloud adapter ($80-250), MELCloud app, WiFi icon on remote
- Modes: Cool snowflake, Heat sun, Dry droplet, Fan pinwheel, Auto loop/A. Fan Low/Med/High/Auto + Quiet moon, Econo Cool, i-Save preset, 3D Auto globe, Plasma diamond/P
- IR: 38kHz, line-of-sight, no pairing. RF/wall needs pairing via MODE/SET + power cycle.

### Mitsubishi Heavy Industries (SRK/SRC — this project)
- RLA502A700B/L (152-bit, SRKxxZM-S/ZS-S, header 52-AE-C3-1A-E5): power/mode/temp/fan/swing/3D/night/silent/clean/turbo/econo
- RKX502A001C (88-bit, SRKxxZJ-S, sig AD-51-3C-D9-26): target family for SRK12CT-S5 verification
- PJA502A704AA (64-bit, 36kHz, not 38kHz): 32-bit data + 32-bit inverted, init 6000/7500
- Features: Temp 18-30C, Fan Auto/Low/Med/High/Turbo/Econo, Swing V (Highest/High/Middle/Low/Lowest/Auto/Swing), Swing H (Left/ML/Middle/MR/Right/Swing/3D), Sleep, Clean, Timer, Econo leaf
- App parity: MELCloud (Electric) vs Comfort Cloud (Heavy) — not used here, IR only

## 2. Architecture
Single-activity Kotlin app, minSdk 24, Jetpack Compose Material3.
`AcState` data class -> `Mhi88Encoder` + `Mhi152Encoder` (fallback flag) -> `IrTransmitter` wrapping `ConsumerIrManager.transmit(freq, pattern[])`.
State persisted via DataStore Preferences. No network permission. No backend.

## 3. Components
- `RemoteScreen.kt`: LCD status bar (mode icon/temp/fan/swing), Power button, Temp -/+, Mode row (Cool/Heat/Dry/Fan/Auto), Fan cycle, Swing V/H, 3D toggle, Sleep/Clean/Timer, Presets (i-Save x2)
- `AcState.kt`: power:Boolean, mode:enum(5), temp:Int 18..30, fan:enum(Auto/Low/Med/High/Turbo/Econo), swingV:enum(7), swingH:enum(9), threeD:Boolean, sleep:Boolean, clean:Boolean
- `Mhi88Encoder.kt`: sig 5 bytes AD 51 3C D9 26, byte9 mode/power/temp nibble, byte7 fan, byte5 swing bits, inverted-byte pairs, footer FF00FF007F80, timings header 3200/1600 mark 400 zero 430 one 1200 at 38000Hz. Ported from IRremoteESP8266 `ir_MitsubishiHeavy.h`.
- `Mhi152Encoder.kt`: behind `use152` debug flag for CT-S5 variant test, sig AD 51 3C E5 1A.
- `IrTransmitter.kt`: `hasIrEmitter()` check, `getCarrierFrequencies()` validation, split patterns over 2 sec limit, `transmit()` on IO dispatcher, returns Result.
- `PresetStore.kt`: 2 slots, save/recall full AcState.

## 4. Data Flow
Tap -> ViewModel updates AcState -> Encoder builds 11-byte frame + checksum -> converts bits to `int[]` on/off microseconds starting with ON -> `transmit(38000, pattern)` synchronous -> UI toast + red LED dot flash. Power-off sends full frame with power bit 0. Temp +/- sends full state frame each time (MHI stateful protocol).

## 5. Error Handling
- No IR emitter: blocking dialog + Demo mode (UI works, transmit mocked, banner shown).
- Frequency unsupported: try 38000 then 36000, warn if both outside `getCarrierFrequencies()`.
- Transmit exception / SecurityException: retry twice, keep prior state, Snackbar with retry.
- Temp clamp 18-30, fan/mode enums prevent invalid frames. Checksum validated in unit tests.
- CT-S5 mismatch (no beep): Settings -> toggle 152-bit test frame, report back.

## 6. Testing
- JUnit: encoder vectors vs IRremoteESP8266 `ir_MitsubishiHeavy_test.cpp` (Cool 24 Auto, Heat 22 Fan3 Swing, Power off), checksum, temp boundaries, bit timings sum under 2 sec.
- Compose UI tests: button presence, temp increment, mode switch updates LCD.
- Manual on Xiaomi: point at SRK12CT-S5, verify beep + louver + display per function checklist (Power, each mode, 18/30 limits, each fan, swing V/H, 3D, sleep, clean).

## 7. Out of Scope v1
No MELCloud/WiFi, no timer scheduling daemon (sends timer bits only), no IR learning importer, no ESP32/Broadlink bridge, no multi-zone addressing.

## 8. Success Criteria
Installs on Xiaomi IR phone, controls SRK12CT-S5 power/temp/mode/fan/swing/3D/sleep/clean from 3m, offline, all unit tests pass.
