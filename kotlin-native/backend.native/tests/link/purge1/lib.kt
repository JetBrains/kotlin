/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.convert
import platform.posix.*

fun foo() {
    println("linked library")
    val size: size_t = 17.convert<size_t>()
    val e = fabs(1.toDouble())
    println("and symbols from posix available: $size; $e")
}
