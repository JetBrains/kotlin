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
    ): List<JsStatement> {
        val parser = initializeParser(fileName, code, 0, CodePosition(0, 0), reporter)
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
        val parser = initializeParser(fileName, code, offset, position, reporter)
        val mapper = JsAstMapper(scope, fileName)
        val function = parser.functionDeclaration()
        return mapper.mapFunction(function)
    }

    private fun initializeParser(
        file: String,
        code: String,
        offset: Int,
        startPosition: CodePosition,
        errorReporter: ErrorReporter
    ): JavaScriptParser {
        val inputStream = CharStreams.fromString(code, file)
        inputStream.seek(offset)
        val offsetStream = OffsetCharStream(
            inputStream,
            startPosition.line,
            startPosition.offset)
        val lexer = JavaScriptLexer(offsetStream).apply {
            removeErrorListeners()
            addErrorListener(errorReporter)
        }

        val tokenStream = CommonTokenStream(lexer)
        return JavaScriptParser(tokenStream).apply {
            removeErrorListeners()
            addErrorListener(errorReporter)
        }
    }
}