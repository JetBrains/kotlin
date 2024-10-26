/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*
import kotlin.native.Platform

fun main(args: Array<String>) {
    // Remove path and extension (.kexe or .exe)
    val programFileName = Platform.programName.substringAfterLast("/").substringBeforeLast(".")

    assertEquals("program_name", programFileName)
}
