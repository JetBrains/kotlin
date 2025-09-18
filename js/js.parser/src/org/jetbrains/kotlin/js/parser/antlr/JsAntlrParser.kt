/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.ErrorReporter
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import org.jetbrains.kotlin.js.backend.ast.JsExpression
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
        return parseStatements(code, fileName, CodePosition(0, 0), 0, reporter, scope) ?: emptyList()
    }

    fun parseExpression(
        code: String,
        fileName: String,
        position: CodePosition,
        offset: Int,
        reporter: ErrorReporter,
        scope: JsScope
    ): JsExpression? {
        return with(JsParserContext(fileName, code, position, offset, scope)) {
            val accReporter = AccumulatingReporter()
            val expression = parseExpression(accReporter)
            pumpInspections(accReporter, reporter)
            expression
        }
    }

    fun parseStatements(
        code: String,
        fileName: String,
        position: CodePosition,
        offset: Int,
        reporter: ErrorReporter,
        scope: JsScope
    ): List<JsStatement>? {
        return with(JsParserContext(fileName, code, position, offset, scope)) {
            val accReporter = AccumulatingReporter()
            val statements = parseStatements(accReporter)
            pumpInspections(accReporter, reporter)
            statements
        }
    }

    fun parseExpressionOrStatement(
        code: String,
        reporter: ErrorReporter,
        scope: JsScope,
        startPosition: CodePosition,
        fileName: String
    ): List<JsStatement>? {
        return with(JsParserContext(fileName, code, startPosition, 0, scope)) {
            val expressionReporter = AccumulatingReporter()
            val expression = parseExpression(expressionReporter)
            if (expression != null && !expressionReporter.hasErrors) {
                pumpInspections(expressionReporter, reporter)
                return listOf(expression.makeStmt())
            }

            val statementsReporter = AccumulatingReporter()
            val statements = parseStatements(statementsReporter)
            pumpInspections(statementsReporter, reporter)
            statements
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
        return with(JsParserContext(fileName, code, position, offset, scope)) {
            val functionReporter = AccumulatingReporter()
            val function = parseFunction(functionReporter)
            pumpInspections(functionReporter, reporter)
            function
        }
    }

    context(parserContext: JsParserContext)
    private fun initializeParser(errorReporter: ErrorReporter): JavaScriptParser {
        val inputStream = CharStreams.fromString(parserContext.code, parserContext.fileName).also {
            it.seek(parserContext.offset)
        }
        val offsetStream = OffsetCharStream(
            inputStream,
            parserContext.startPosition.line,
            parserContext.startPosition.offset)
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

    private fun pumpInspections(from: AccumulatingReporter, to: ErrorReporter) {
        for (warning in from.warnings) {
            to.warning(warning.message, warning.startPosition, warning.endPosition)
        }
        for (error in from.errors) {
            to.error(error.message, error.startPosition, error.endPosition)
        }
    }

    context(parserContext: JsParserContext)
    private fun parseExpression(accReporter: AccumulatingReporter) = parseAndMap(
        accReporter,
        parseFunc = { parser -> parser.singleExpression() },
        mapFunc = { expression -> JsAstMapper(parserContext.scope, parserContext.fileName, accReporter).mapExpression(expression) }
    )

    context(parserContext: JsParserContext)
    private fun parseStatements(accReporter: AccumulatingReporter) = parseAndMap(
        accReporter,
        parseFunc = { parser -> parser.statementList()?.statement() },
        mapFunc = { statements ->
            statements.filterNotNull().map { JsAstMapper(parserContext.scope, parserContext.fileName, accReporter).mapStatement(it) }
        }
    )

    context(parserContext: JsParserContext)
    private fun parseFunction(accReporter: AccumulatingReporter) = parseAndMap(
        accReporter,
        parseFunc = { parser -> parser.functionDeclaration() },
        mapFunc = { function -> JsAstMapper(parserContext.scope, parserContext.fileName, accReporter).mapFunction(function) }
    )

    context(parserContext: JsParserContext)
    private fun <TParseResult, TMapResult> parseAndMap(
        reporter: AccumulatingReporter,
        parseFunc: (JavaScriptParser) -> TParseResult?,
        mapFunc: (TParseResult) -> TMapResult
    ): TMapResult? {
        try {
            val parser = initializeParser(reporter)
            val parsedResult = parseFunc(parser) ?: return null
            if (parser.currentToken.type != Token.EOF) {
                reporter.error("Unexpected token '${parser.currentToken}'", parserContext.startPosition, parserContext.startPosition)
            }
            if (reporter.hasErrors) {
                return null
            }
            return mapFunc(parsedResult)
        } catch (ex: Throwable) {
            reporter.error("Failed to parse: ${ex.message}", parserContext.startPosition, parserContext.startPosition)
            return null
        }
    }

    private class AccumulatingReporter : ErrorReporter {
        var hasErrors = false
        val errors = mutableListOf<Error>()
        val warnings = mutableListOf<Warning>()

        override fun warning(message: String, startPosition: CodePosition, endPosition: CodePosition) {
            warnings += Warning(message, startPosition, endPosition)
        }

        override fun error(message: String, startPosition: CodePosition, endPosition: CodePosition) {
            hasErrors = true
            errors += Error(message, startPosition, endPosition)
        }

        class Error(val message: String, val startPosition: CodePosition, val endPosition: CodePosition)
        class Warning(val message: String, val startPosition: CodePosition, val endPosition: CodePosition)
    }

    private data class JsParserContext(
        val fileName: String,
        val code: String,
        val startPosition: CodePosition,
        val offset: Int,
        val scope: JsScope
    )
}