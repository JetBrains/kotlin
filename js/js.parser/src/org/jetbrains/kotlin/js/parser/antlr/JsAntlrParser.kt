/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.ErrorReporter
import com.google.gwt.dev.js.rhino.JavaScriptException
import com.google.gwt.dev.js.rhino.TokenStream
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
        val accumulatingReporter = AccumulatingReporter()
        val parser = initializeParser(fileName, code, 0, startPosition, accumulatingReporter)
        val mapper = JsAstMapper(scope, fileName)
        val expr = try {
            val expression = parser.singleExpression()
            if (parser.currentToken.type != TokenStream.EOF) {
                accumulatingReporter.hasErrors = true
            }
            expression
        } catch (_: JavaScriptException) {
            null
        }

        return if (!accumulatingReporter.hasErrors) {
            for (warning in accumulatingReporter.warnings) {
                reporter.warning(warning.message, warning.startPosition, warning.endPosition)
            }
            val jsExpr = expr?.let { mapper.mapExpression(it) }
            jsExpr?.makeStmt()?.let(::listOf)
        } else {
            // Re-create parser to reset lexer and stream state back to the initial offset and to pass the real reporter instance
            val parser = initializeParser(fileName, code, 0, startPosition, reporter)
            val statements = parser.statementList()
            statements.statement().map {
                mapper.mapStatement(it)
            }
        }
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
        val inputStream = CharStreams.fromString(code, file).also {
            it.seek(offset)
        }
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

    private class AccumulatingReporter : ErrorReporter {
        var hasErrors = false
        val warnings = mutableListOf<Warning>()

        override fun warning(message: String, startPosition: CodePosition, endPosition: CodePosition) {
            warnings += Warning(message, startPosition, endPosition)
        }

        override fun error(message: String, startPosition: CodePosition, endPosition: CodePosition) {
            hasErrors = true
        }

        class Warning(val message: String, val startPosition: CodePosition, val endPosition: CodePosition)
    }
}