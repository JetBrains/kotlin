/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.debug

typealias DebugInformation = List<DebugSection>

class DebugSection(val name: String, val data: DebugData)

sealed interface DebugData {
    @JvmInline
    value class StringData(val value: String) : DebugData
}