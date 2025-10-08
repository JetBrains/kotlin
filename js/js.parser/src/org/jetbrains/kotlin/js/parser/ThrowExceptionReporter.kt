/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser

object ThrowExceptionOnErrorReporter : ErrorReporter {
    override fun warning(message: String, startPosition: CodePosition, endPosition: CodePosition) {}

    override fun error(message: String, startPosition: CodePosition, endPosition: CodePosition) =
        throw JsParserException(message, startPosition)
}