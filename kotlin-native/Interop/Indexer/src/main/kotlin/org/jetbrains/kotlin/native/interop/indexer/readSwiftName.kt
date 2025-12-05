/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.*

internal fun readSwiftName(cursor: CValue<CXCursor>): String? {
    val ptr = clang_Cursor_getSwiftName(cursor) ?: return null
    try {
        return ptr.toKString()
    } finally {
        clang_free(ptr)
    }
}