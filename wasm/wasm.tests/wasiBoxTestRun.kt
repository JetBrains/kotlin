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

@kotlin.wasm.WasmExport
fun startTest() {
    try {
        if (!runBoxTest()) {
            throw Exception("Tests failed")
        }
    } catch (e: Throwable) {
        println("Failed with exception!")
        println(e.message)
        println(e.printStackTrace())
        throw e
    }
}