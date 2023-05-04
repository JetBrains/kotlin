/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cstdlib.*
import kotlinx.cinterop.*

fun main(args: Array<String>) {
    println(atoi("257"))

    val divResult = div(-5, 3)
    val (quot, rem) = divResult.useContents { Pair(quot, rem) }
    println(quot)
    println(rem)

}
