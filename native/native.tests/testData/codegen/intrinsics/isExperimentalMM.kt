/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*
import kotlin.native.Platform

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun box(): String {
    if (isExperimentalMM()) {
        assertEquals(Platform.memoryModel, MemoryModel.EXPERIMENTAL)
    } else {
        assertNotEquals(Platform.memoryModel, MemoryModel.EXPERIMENTAL)
    }
    return "OK"
}