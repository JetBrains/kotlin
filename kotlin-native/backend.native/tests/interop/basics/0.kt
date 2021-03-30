/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import sysstat.*
import kotlinx.cinterop.*

fun main(args: Array<String>) {
    val statBuf = nativeHeap.alloc<stat>()
    val res = stat("/", statBuf.ptr)
    println(res)
    println(statBuf.st_uid)
}