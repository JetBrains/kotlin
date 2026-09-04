/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@kotlin.wasm.WasmExport
fun runBoxTest(): Boolean {
    // The source provider can only identify the `box()` declaration syntactically before analysis is available. Keep
    // inferred-String expression bodies supported, but make an inferred non-String return an explicit compile error.
    val boxResult: String = box()
    val isOk = boxResult == "OK"
    if (!isOk) {
        println("Wrong box result '${boxResult}'; Expected 'OK'")
    }
    return isOk
}

@kotlin.wasm.WasmImport("wasi_snapshot_preview1", "proc_exit")
private external fun wasiProcExit(code: Int)

// This `startTest()` shares its export name with the grouped batches' result-collecting driver on purpose; the two never
// end up in the same binary, see `WasmWasiGroupedTestsExportedEntryPointGenerator`.
@kotlin.wasm.WasmExport
fun startTest() {
    try {
        if (!runBoxTest()) {
            wasiProcExit(1)
        }
    } catch (e: Throwable) {
        println("Failed with exception!")
        println(e)
        wasiProcExit(1)
    }
}
