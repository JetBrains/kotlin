/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * Write high-quality random data into a buffer. This function blocks when the implementation is
 * unable to immediately provide sufficient high-quality random data. This function may execute
 * slowly, so when large mounts of random data are required, it's advisable to use this function to
 * seed a pseudo-random number generator, rather than to provide the random data directly.
 */
@WasmImport("wasi_snapshot_preview1", "random_get")
//TODO: Better to use 64-bit Long as a seed if possible. (KT-60962)
internal external fun wasiRawRandomGet(address: Int, size: Int): Int

private fun wasiRandomGet(): Int {
    withScopedMemoryAllocator { allocator ->
        val memory = allocator.allocate(Int.SIZE_BYTES)
        val ret = wasiRawRandomGet(memory.address.toInt(), Int.SIZE_BYTES)
        return if (ret == 0) {
            memory.loadInt()
        } else {
            throw WasiError(WasiErrorCode.values()[ret])
        }
    }
}

internal actual fun defaultPlatformRandom(): Random =
    Random(wasiRandomGet())