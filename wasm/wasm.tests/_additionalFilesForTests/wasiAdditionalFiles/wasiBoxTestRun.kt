/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This `startTest` shares its export name with the grouped batches' result-collecting driver
// (`WasmWasiGroupedTestsExportedEntryPointGenerator`) on purpose, and the two never collide: this file is attached to
// every per-test compilation, but in a grouped batch the per-test KLIB is an ordinary `-libraries` dependency, whose
// unreferenced declarations never enter the link — so a grouped binary exports only the driver's `startTest`, while an
// isolated binary (this KLIB as the `-Xinclude` main module) exports this one. The invariant is enforced at run time
// by `assertDriverOwnsStartTestExport`; see the KDoc of `WasmWasiBoxTestHelperSourceProvider` for why this file cannot
// simply be omitted from batched tests instead.

@kotlin.wasm.WasmExport
fun runBoxTest(): Boolean {
    val boxResult = box()
    val isOk = boxResult == "OK"
    if (!isOk) {
        println("Wrong box result '${boxResult}'; Expected 'OK'")
    }
    return isOk
}

@kotlin.wasm.WasmImport("wasi_snapshot_preview1", "proc_exit")
private external fun wasiProcExit(code: Int)

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
