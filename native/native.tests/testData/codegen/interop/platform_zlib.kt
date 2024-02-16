/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TARGET_BACKEND: NATIVE
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*
import kotlin.test.assertEquals

import platform.zlib.*

val source = immutableBlobOf(0xF3, 0x48, 0xCD, 0xC9, 0xC9, 0x57, 0x04, 0x00).asCPointer().reinterpret<UByteVar>()
val golden = immutableBlobOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21, 0x00).asCPointer()

fun box() = memScoped {
    val buffer = ByteArray(32)
    buffer.usePinned { pinned ->
        val z = alloc<z_stream>().apply {
            next_in = source
            avail_in = 8u
            next_out = pinned.addressOf(0).reinterpret()
            avail_out = buffer.size.toUInt()
        }.ptr

        if (inflateInit2(z, -15) == Z_OK && inflate(z, Z_FINISH) == Z_STREAM_END && inflateEnd(z) == Z_OK)
            assertEquals("Hello!", buffer.toKString())
    }
    assertEquals("Hello!", golden.toKString())
    "OK"
}
