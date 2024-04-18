/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.random.wasiRawRandomGet
import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.unsafe.withScopedMemoryAllocator

internal actual fun secureRandomUUID(): UUID {
    withScopedMemoryAllocator { allocator ->
        var memory = allocator.allocate(UUID.SIZE_BYTES)
        val ret = wasiRawRandomGet(memory.address.toInt(), UUID.SIZE_BYTES)
        return if (ret == 0) {
            val msb = memory.loadLong()
            memory += Long.SIZE_BYTES
            val lsb = memory.loadLong()
            uuidFromRandomLongs(msb, lsb)
        } else {
            throw WasiError(WasiErrorCode.entries[ret])
        }
    }
}

internal fun uuidFromRandomLongs(msb: Long, lsb: Long): UUID {
    var _msb = msb and 0xf000L.inv()        /* clear version        */
    _msb = _msb or 0x4000                   /* set to version 4     */
    var _lsb = lsb and 0x3fffffffffffffff   /* clear variant        */
    _lsb = _lsb or 0x7fffffffffffffff.inv() /* set to IETF variant  */
    return UUID.fromLongs(_msb, _lsb)
}
