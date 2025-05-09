/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.*


internal fun readSwiftName(cursor: CValue<CXCursor>): String? {
    var result: String? = null
    visitChildren(cursor) { child, _ ->
        val toKString = clang_Cursor_getAttributeSpelling(child)?.toKString()
        if (clang_isAttribute(child.kind) != 0 && toKString == "swift_name") {
            val tu = clang_Cursor_getTranslationUnit(child)!!
            val extent = clang_getCursorExtent(child)
            val rangeStart = clang_getRangeStart(extent)
            val rangeEnd = clang_getRangeEnd(extent)

            val fullText = getText(tu, rangeStart, rangeEnd)
            if (fullText != null) {
                result = fullText.substringAfter("\"").substringBefore("\"")
            }
        }
        CXChildVisitResult.CXChildVisit_Continue
    }
    return result
}

private fun getText(tu: CXTranslationUnit, start: CValue<CXSourceLocation>, end: CValue<CXSourceLocation>): String? = memScoped {
    val tokensVar = alloc<CPointerVar<CXToken>>()
    val numTokensVar = alloc<IntVar>()
    clang_tokenize(tu, clang_getRange(start, end), tokensVar.ptr, numTokensVar.ptr)
    val numTokens = numTokensVar.value
    val tokens = tokensVar.value ?: return null
    try {
        (0 until numTokens).joinToString("") { i ->
            clang_getTokenSpelling(tu, tokens[i].readValue()).convertAndDispose()
        }
    } finally {
        clang_disposeTokens(tu, tokens, numTokens)
    }
}