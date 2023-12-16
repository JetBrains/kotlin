/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlin.test.*
import kotlinx.cinterop.Vector128
import kotlinx.cinterop.vectorOf

private fun funDefaultValue(v: Vector128 = vectorOf(1.0f, 2.0f, 3.0f, 4.0f)) = v

fun box(): String {
    assertEquals(vectorOf(1.0f, 2.0f, 3.0f, 4.0f), funDefaultValue())
    return "OK"
}