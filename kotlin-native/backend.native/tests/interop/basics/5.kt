/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import platform.posix.printf

val golden = immutableBlobOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21, 0x00).asCPointer(0)

fun main(args: Array<String>) {
    printf("%s\n", golden)
}
