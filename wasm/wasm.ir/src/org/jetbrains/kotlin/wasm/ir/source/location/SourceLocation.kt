/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.source.location

sealed class SourceLocation {
    object NoLocation: SourceLocation()
    object TBDLocation: SourceLocation()

    data class Location(val file: String, val line: Int, val column: Int) : SourceLocation()
}
