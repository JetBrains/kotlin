/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@kotlin.wasm.WasmExport
fun runBoxTest(): Boolean {
    val boxResult = box()
    val isOk = boxResult == "OK"
    if (!isOk) {
        println("Wrong box result '${boxResult}'; Expected 'OK'")
    }
    return isOk
}

// HACK (KT-87723):
// - kebab-case export name, so that it can be named in the WIT world the test binary is embedded with (see
//   WasiComponentizer.BOX_ENTRY_POINT); WIT and `wasmtime --invoke` (WAVE) reject camelCase names,
// - failures escape as a trap (i.e. a non-zero exit code, which is what the test runner checks) instead of calling
//   the preview1 `proc_exit`, which would make this binary need a preview1 adapter to become a component.
@kotlin.wasm.WasmExport("start-test")
fun startTest() {
    val isOk = try {
        runBoxTest()
    } catch (e: Throwable) {
        println("Failed with exception!")
        println(e)
        false
    }
    if (!isOk) error("Box test failed")
}
