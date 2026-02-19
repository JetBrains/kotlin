/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// OUTPUT_DATA_FILE: puts.out
// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cstdio.def
headers = stdio.h

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import cstdio.*

fun main(args: Array<String>) {
    puts("Hello")
}