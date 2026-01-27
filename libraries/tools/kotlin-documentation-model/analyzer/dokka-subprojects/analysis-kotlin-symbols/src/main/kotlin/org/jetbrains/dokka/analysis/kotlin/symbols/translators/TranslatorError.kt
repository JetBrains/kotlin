/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol

internal class TranslatorError(message: String, cause: Throwable?) : IllegalStateException(message, cause)

internal inline fun <R> KaSession.withExceptionCatcher(symbol: KaSymbol, action: KaSession.() -> R): R =
    try {
        action()
    } catch (e: TranslatorError) {
        throw e
    } catch (e: Throwable) {
        val filePath = try {
            symbol.psi?.containingFile?.virtualFile?.path
        } catch (e: Throwable) {
            "[$e]"
        }
        val lineSuffix = try {
            val offset = symbol.psi?.textOffset
            val fileDocument = symbol.psi?.containingFile?.fileDocument
            val lineNumber = offset?.let { fileDocument?.getLineNumber(it) }
            if (lineNumber != null) ":${lineNumber + 1}"
            else ""
        } catch (e: Throwable) {
            "[$e]"
        }
        throw TranslatorError(
            "Error in translating of symbol (${(symbol as? KaNamedSymbol)?.name}) $symbol in file:///$filePath$lineSuffix",
            e
        )
    }
