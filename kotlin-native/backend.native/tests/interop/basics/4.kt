/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cstdio.*
import kotlinx.cinterop.*

val stdout
    get() = getStdout()

fun main(args: Array<String>) {
    fprintf(stdout, "%s %s %d %d %d %lld %.1f %.1lf %d %d\n",
            "a", "b".cstr, (-1).toByte(), 2.toShort(), 3, Long.MAX_VALUE, 0.1.toFloat(), 0.2, true, false)

    memScoped {
        val aVar = alloc<IntVar>()
        val bVar = alloc<IntVar>()
        val sscanfResult = sscanf("42", "%d%d", aVar.ptr, bVar.ptr)
        printf("%d %d\n", sscanfResult, aVar.value)
    }
}
