// Wasm/WASI-specific declarations: @WasmImport and @WasmExport.

// MODULE: wasmWasiMain
// PLATFORM: WASM_WASI
// FILE: pkg/WasmWasi.kt
@file:OptIn(ExperimentalWasmInterop::class)

package pkg

import kotlin.wasm.WasmExport
import kotlin.wasm.WasmImport

@WasmImport("wasi_snapshot_preview1", "random_get")
external fun randomGet(address: Int, length: Int): Int

@WasmExport("wasi_add")
fun add(a: Int, b: Int): Int = a + b

class WasiClock(val resolution: Long)
