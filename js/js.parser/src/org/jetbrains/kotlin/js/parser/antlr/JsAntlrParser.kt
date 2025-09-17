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
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeVisitor
import org.antlr.v4.runtime.tree.RuleNode
import org.antlr.v4.runtime.tree.TerminalNode
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
        val parser = initializeParser(fileName, code, 0, CodePosition(0, 0), reporter)
        val mapper = JsAstMapper(scope, fileName)
        val statements = parser.statementList()
        val jsStatements = statements.statement().filterNotNull().map {
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
        fun <TParseResult, TMapResult> parseWholeAndMap(
            reporter: AccumulatingReporter,
            parseFunc: (JavaScriptParser) -> TParseResult?,
            mapFunc: (TParseResult) -> TMapResult
        ): TMapResult? {
            try {
                val parser = initializeParser(fileName, code, 0, startPosition, reporter)
                val parsedResult = parseFunc(parser) ?: return null
                if (parser.currentToken.type != Token.EOF) {
                    reporter.error("Unexpected token '${parser.currentToken}'", startPosition, startPosition)
                }
                if (reporter.hasErrors) {
                    return null
                }
                return mapFunc(parsedResult)
            } catch (ex: Throwable) {
                reporter.error("Failed to parse: ${ex.message}", startPosition, startPosition)
                return null
            }
        }

        fun parseExpression(accReporter: AccumulatingReporter) = parseWholeAndMap(
            accReporter,
            parseFunc = { parser -> parser.singleExpression() },
            mapFunc = { expression -> JsAstMapper(scope, fileName).mapExpression(expression) }
        )

        fun parseStatements(accReporter: AccumulatingReporter) = parseWholeAndMap(
            accReporter,
            parseFunc = { parser -> parser.statementList()?.statement() },
            mapFunc = { statements -> statements.filterNotNull().map { JsAstMapper(scope, fileName).mapStatement(it) } }
        )

        fun pumpInspections(from: AccumulatingReporter, to: ErrorReporter) {
            for (warning in from.warnings) {
                to.warning(warning.message, warning.startPosition, warning.endPosition)
            }
            for (error in from.errors) {
                to.error(error.message, error.startPosition, error.endPosition)
            }
        }

        val expressionReporter = AccumulatingReporter()
        val expression = parseExpression(expressionReporter)
        if (expression != null && !expressionReporter.hasErrors) {
            pumpInspections(expressionReporter, reporter)
            return listOf(expression.makeStmt())
        }

        val statementsReporter = AccumulatingReporter()
        val statements = parseStatements(statementsReporter)
        pumpInspections(statementsReporter, reporter)
        return statements
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
}