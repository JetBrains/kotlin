/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@kotlin.wasm.WasmExport
fun runBoxTest(): Boolean {
    return box() == "OK"
}

@kotlin.wasm.WasmImport("ssw_util", "proc_exit")
private external fun procExit(code: Int)

@kotlin.wasm.WasmExport
fun startTest() {
    if (!runBoxTest()) {
        procExit(1)
    }
}