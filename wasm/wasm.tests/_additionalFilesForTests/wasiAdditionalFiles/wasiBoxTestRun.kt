/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@kotlin.wasm.WasmExport
fun runBoxTest(): Boolean {
    val boxResult: String = box()
    val isOk = boxResult == "OK"
    if (!isOk) {
        println("Wrong box result '${boxResult}'; Expected 'OK'")
    }
    return isOk
}

// only takes 1 for failure, 0 for success
// (originally stems from a WIT result<> type (without payload), which the canonical abi lowers to a single i32 representing failure or success)
@kotlin.wasm.WasmImport("wasi:cli/exit@0.2.12", "exit")
private external fun wasiExit(status: Int)

// This `starttest` has the same export name as the result-collecting driver of grouped batches. The two are never in the
// same binary, see `wasmWasiGroupedTestsEntryPointSource`.

@kotlin.wasm.WasmExport("starttest")
fun startTest() {
    try {
        if (!runBoxTest()) {
            wasiExit(1)
        }
    } catch (e: Throwable) {
        println("Failed with exception!")
        println(e)
        wasiExit(1)
    }
}
