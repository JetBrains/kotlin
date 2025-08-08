/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import JavaScriptLexer
import JavaScriptParser
import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.ErrorReporter
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.backend.ast.JsStatement

object JsAntlrParser {
    fun parse(
        code: String,
        reporter: ErrorReporter,
        scope: JsScope,
        fileName: String
    ): List<JsStatement>? {
        val parser = createJsParser(code, )
    }

    fun parseExpressionOrStatement(
        code: String,
        reporter: ErrorReporter,
        scope: JsScope,
        startPosition: CodePosition,
        fileName: String
    ): List<JsStatement>? {

    }

    fun parseFunction(
        code: String,
        fileName: String,
        position: CodePosition,
        offset: Int,
        reporter: ErrorReporter,
        scope: JsScope
    ): JsFunction? {

    }

    private fun createJsParser(file: String, code: String, startPosition: CodePosition): JavaScriptParser {
        val inputStream = CharStreams.fromString(code, file)
        val offsetStream = OffsetCharStream(
            inputStream,
            startPosition.line,
            startPosition.offset)
        val lexer = JavaScriptLexer(offsetStream)
        val tokenStream = CommonTokenStream(lexer)
        return JavaScriptParser(tokenStream)
    }
}