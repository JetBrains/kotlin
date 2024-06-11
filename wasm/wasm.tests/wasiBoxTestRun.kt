/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@kotlin.wasm.WasmExport
fun runBoxTest(): Boolean {
    val boxResult = box() //TODO: Support non-root package box functions
    val isOk = boxResult == "OK"
    if (!isOk) {
        println("Wrong box result '${boxResult}'; Expected 'OK'")
    }
    return isOk
}

@kotlin.wasm.WasmImport("wasi_snapshot_preview1", "proc_exit")
private external fun wasiProcExit(code: Int)

@kotlin.wasm.WasmExport("__start")
fun start() {
    var code: Int = 0

    try {
        if (!runBoxTest()) {
            code = 1
        }
    } catch (e: Throwable) {
        code = 1
    }
    wasiProcExit(code)
}