/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.js.parser.antlr.AntlrJsCommentsCollector
import org.jetbrains.kotlin.js.parser.antlr.AntlrJsValidationVisitor
import org.jetbrains.kotlin.js.parser.antlr.JsAstMapper
import org.jetbrains.kotlin.js.parser.antlr.addErrorListener
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptLexer
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser

object JsParser {
    fun parse(
        code: String,
        reporter: ErrorReporter,
        scope: JsScope,
        fileName: String
    ): List<JsStatement> {
        return parseStatements(code, fileName, CodePosition(0, 0), 0, reporter, scope) ?: emptyList()
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
            val statementsReporter = AccumulatingReporter()
            val parser = initializeParser(statementsReporter)
            val statements = parseAndMap(statementsReporter, StatementsParserProfile(parser, this, reporter))
            pumpInspections(statementsReporter, reporter)
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
            val statementsReporter = AccumulatingReporter()
            val parser = initializeParser(statementsReporter)
            val statements = parseAndMap(statementsReporter, ExpressionOrStatementsParserProfile(parser, this, reporter))
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
            val parser = initializeParser(functionReporter)
            val function = parseAndMap(functionReporter, FunctionParserProfile(parser, this, reporter))
            pumpInspections(functionReporter, reporter)
            function
        }
    }

    context(parserContext: JsParserContext)
    private fun initializeParser(errorReporter: ErrorReporter): JavaScriptParser {
        val inputStream = CharStreams.fromString(parserContext.code, parserContext.fileName).also {
            it.seek(parserContext.offset)
        }
        val lexer = JavaScriptLexer(inputStream).apply {
            setTokenOffset(parserContext.startPosition.line, parserContext.startPosition.offset)
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
    private fun <TParseResult, TMapResult> parseAndMap(
        reporter: AccumulatingReporter,
        profile: ParserProfile<TParseResult, TMapResult>
    ): TMapResult? {
        try {
            val parsedResult = profile.parse() ?: return null
            if (reporter.hasErrors) {
                return null
            }
            profile.validate(parsedResult)
            return profile.map(parsedResult)
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

    private interface ParserProfile<TParseResult, TMapResult> {
        fun parse(): TParseResult?
        fun validate(result: TParseResult)
        fun map(result: TParseResult): TMapResult
    }

    private class StatementsParserProfile(
        private val parser: JavaScriptParser,
        private val parserContext: JsParserContext,
        private val reporter: ErrorReporter,
    ) : ParserProfile<List<JavaScriptParser.StatementContext?>, List<JsStatement>> {
        override fun parse(): List<JavaScriptParser.StatementContext?>? {
            return parser.optionalStatements().statement()?.also { statements ->
                val commentsCollector = AntlrJsCommentsCollector(parser.tokenStream as CommonTokenStream)
                statements.forEach { commentsCollector.visit(it) }
            }
        }

        override fun validate(result: List<JavaScriptParser.StatementContext?>) {
            result.forEach {
                it?.accept(AntlrJsValidationVisitor(reporter))
            }
        }

        override fun map(result: List<JavaScriptParser.StatementContext?>): List<JsStatement> =
            result.filterNotNull().map {
                JsAstMapper(parserContext.scope, parserContext.fileName, reporter).mapStatement(it)
            }
    }

    private class ExpressionOrStatementsParserProfile(
        private val parser: JavaScriptParser,
        private val parserContext: JsParserContext,
        private val reporter: ErrorReporter,
    ) : ParserProfile<ExpressionOrStatementsParserProfile.ExpressionOrStatements, List<JsStatement>> {
        override fun parse(): ExpressionOrStatements? {
            val result = parser.optionalExpressionOrStatement()
            val expression = result.singleExpression()
            val statements = result.statementList()?.statement()?.filterNotNull()

            expression?.let {
                val commentsCollector = AntlrJsCommentsCollector(parser.tokenStream as CommonTokenStream)
                commentsCollector.visit(it)
                return ExpressionOrStatements(it, null)
            }

            statements?.let {
                val commentsCollector = AntlrJsCommentsCollector(parser.tokenStream as CommonTokenStream)
                it.forEach { statement -> commentsCollector.visit(statement) }
                return ExpressionOrStatements(null, it)
            }

            return null
        }

        override fun validate(result: ExpressionOrStatements) {
            result.expression?.accept(AntlrJsValidationVisitor(reporter))
            result.statements?.forEach { it?.accept(AntlrJsValidationVisitor(reporter)) }
        }

        override fun map(result: ExpressionOrStatements): List<JsStatement> {
            result.expression?.let { expression ->
                return JsAstMapper(parserContext.scope, parserContext.fileName, reporter)
                    .mapExpression(expression)
                    .makeStmt()
                    .let(::listOf)
            }

            result.statements?.let { statements ->
                return statements.filterNotNull().map {
                    JsAstMapper(parserContext.scope, parserContext.fileName, reporter).mapStatement(it)
                }
            }

            error("Either expression or statements should be available for mapping")
        }

        private data class ExpressionOrStatements(
            val expression: JavaScriptParser.SingleExpressionContext?,
            val statements: List<JavaScriptParser.StatementContext?>?
        )
    }

    private class FunctionParserProfile(
        private val parser: JavaScriptParser,
        private val parserContext: JsParserContext,
        private val reporter: ErrorReporter,
    ) : ParserProfile<JavaScriptParser.FunctionDeclarationContext, JsFunction> {
        override fun parse(): JavaScriptParser.FunctionDeclarationContext? {
            return parser.optionalFunction().functionDeclaration()?.also {
                val commentsCollector = AntlrJsCommentsCollector(parser.tokenStream as CommonTokenStream)
                commentsCollector.visit(it)
            }
        }

        override fun validate(result: JavaScriptParser.FunctionDeclarationContext) {
            result.accept(AntlrJsValidationVisitor(reporter))
        }

        override fun map(result: JavaScriptParser.FunctionDeclarationContext): JsFunction =
            JsAstMapper(parserContext.scope, parserContext.fileName, reporter).mapFunction(result)
    }
}