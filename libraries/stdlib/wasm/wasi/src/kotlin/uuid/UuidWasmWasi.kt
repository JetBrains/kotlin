/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

import kotlin.random.wasiRawRandomGet
import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.unsafe.withScopedMemoryAllocator

internal actual fun secureRandomBytes(destination: ByteArray): Unit {
    withScopedMemoryAllocator { allocator ->
        var memory = allocator.allocate(destination.size)
        @OptIn(ExperimentalWasmInterop::class)
        val ret = wasiRawRandomGet(memory.address.toInt(), destination.size)
        return if (ret == 0) {
            for (idx in destination.indices) {
                destination[idx] = memory.loadByte()
                memory += 1
            }
        } else {
            throw RuntimeException(cause = WasiError(WasiErrorCode.entries[ret]))
        }
    }
}
