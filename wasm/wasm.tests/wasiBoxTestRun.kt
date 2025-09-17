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

@kotlin.wasm.WasmImport("ssw_util", "proc_exit")
private external fun procExit(code: Int)

@kotlin.wasm.WasmExport
fun startTest() {
    try {
        if (!runBoxTest()) {
            procExit(1)
        }
    } catch (e: Throwable) {
        println("Failed with exception!")
        println(e.message)
        println(e.printStackTrace())
        procExit(1)
    }
}