/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.ErrorReporter
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptLexer
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser

object JsAntlrParser {
    fun parse(
        code: String,
        reporter: ErrorReporter,
        scope: JsScope,
        fileName: String
    ): List<JsStatement>? {
        val parser = createJsParser(fileName, code, CodePosition(0, 0)).apply {
            addErrorListener(reporter)
        }
        val mapper = JsAstMapper(scope, fileName)
        val statements = parser.statementList()
        val jsStatements = statements.statement().map {
            mapper.mapStatement(it)
                ?: throw Exception("unexpected null statement")
        }

        return jsStatements
    }

    fun parseExpressionOrStatement(
        code: String,
        reporter: ErrorReporter,
        scope: JsScope,
        startPosition: CodePosition,
        fileName: String
    ): List<JsStatement>? {
        TODO()
    }

    fun parseFunction(
        code: String,
        fileName: String,
        position: CodePosition,
        offset: Int,
        reporter: ErrorReporter,
        scope: JsScope
    ): JsFunction? {
        val parser = createJsParser(fileName, code, position).apply {
            addErrorListener(reporter)
        }
        val mapper = JsAstMapper(scope, fileName)
        val function = parser.functionDeclaration()
        return mapper.mapFunction(function)
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