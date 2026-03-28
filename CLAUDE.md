# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Smart Lamp is an IoT system for controlling RGB LEDs via Bluetooth Low Energy (BLE). It consists of two components:

1. **ESP32 Firmware** (`esp32-firmware/`) — Arduino/C++ BLE server that controls 3 RGB LEDs via PWM. Each LED has its own RX/TX BLE characteristic pair. Receives brightness commands (0-255 as UTF-8 strings) and sends notifications back.
2. **Android App** (`AndroidSmartLamp/`) — Kotlin Android app that discovers and controls lamps entirely via BLE. Uses manual dependency injection via `DependencyContainer`, SQLite database, and a single Home screen.

## Architecture

### Communication Flow
Android App → BLE (GATT write to RX characteristic) → ESP32 → PWM → RGB LEDs

No backend server or internet required. The phone communicates directly with the ESP32 over Bluetooth.

### How Lamp Identification Works
The ESP32 exposes 1 BLE service with 3 RX/TX characteristic pairs (one per LED). Each LED is uniquely identified by its RX characteristic UUID. The Android app discovers these UUIDs during BLE service discovery and stores them in the local SQLite database.

### Android App Architecture
- **DI**: `DependencyContainer` (manual singleton) provides `LampService` → `BluetoothManager` + `LampRepository`
- **ViewModel**: `LampVM` created via `LampVMFactory`, injected with `LampService`
- **Database**: SQLite via `AppDatabase` (SQLiteOpenHelper). Lamp data persists across app restarts.
- **BLE Flow**: Scan → find ESP32 → connect → discover 3 LED characteristics → auto-create Lamp entries in DB → control via RX writes
- **Auto-reconnect**: On launch, attempts to reconnect to last known ESP32 MAC (saved in SharedPreferences)
- **Package**: `com.iot.android.smartlamp`
- **Min SDK**: 34, Target/Compile SDK: 36

### ESP32 Firmware
- Advertises as "ESP32-Smart-Lamp"
- 3 RGB LEDs on GPIO pins: LED1 (15,2,4), LED2 (5,18,19), LED3 (21,22,23)
- PWM: 5000 Hz, 8-bit resolution
- BLE characteristics created in pairs: TX (NOTIFY) then RX (WRITE) per LED

## Build & Run Commands

### ESP32 Firmware
Upload via Arduino IDE (v2.2.1 recommended). Board: ESP32 WROOM-32. Requires BLE libraries (included with ESP32 board package).

### Android App
```bash
cd AndroidSmartLamp
./gradlew assembleDebug        # Build debug APK
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests
```
Requires JDK 11, Gradle 8.3, Android SDK 36.

## Key Dependencies
- **Android**: Gson (color parsing), ColorPickerView, Shimmer (loading effects), SwipeRefreshLayout
- **ESP32**: BLE libraries (BLEDevice, BLEServer, BLEUtils, BLE2902)

## Branch Strategy
- `main` — stable/documentation branch
- `v3` — active development branch (simplified offline BLE-only architecture)
