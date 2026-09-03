/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.js.util.TextOutput
import java.math.BigInteger

open class JsToStringGenerationVisitor(
    protected val p: TextOutput,
    private val sourceLocationConsumer: SourceLocationConsumer = NoOpSourceLocationConsumer,
) : JsVisitor() {
    companion object {
        private val CHARS_BREAK = "break".toCharArray()
        private val CHARS_CASE = "case".toCharArray()
        private val CHARS_CATCH = "catch".toCharArray()
        private val CHARS_CLASS = "class".toCharArray()
        private val CHARS_CONSTRUCTOR = "constructor".toCharArray()
        private val CHARS_CONTINUE = "continue".toCharArray()
        private val CHARS_YIELD = "yield".toCharArray()
        private val CHARS_YIELD_STAR = "yield*".toCharArray()
        private val CHARS_DEBUGGER = "debugger".toCharArray()
        private val CHARS_DEFAULT = "default".toCharArray()
        private val CHARS_DO = "do".toCharArray()
        private val CHARS_ELSE = "else".toCharArray()
        private val CHARS_EXTENDS = "extends".toCharArray()
        private val CHARS_FALSE = "false".toCharArray()
        private val CHARS_FINALLY = "finally".toCharArray()
        private val CHARS_FOR = "for".toCharArray()
        private val CHARS_FUNCTION = "function".toCharArray()
        private val CHARS_STATIC = "static".toCharArray()
        private val CHARS_GET = "get".toCharArray()
        private val CHARS_SET = "set".toCharArray()
        private val CHARS_IF = "if".toCharArray()
        private val CHARS_IN = "in".toCharArray()
        private val CHARS_OF = "of".toCharArray()
        private val CHARS_NEW = "new".toCharArray()
        private val CHARS_NULL = "null".toCharArray()
        private val CHARS_RETURN = "return".toCharArray()
        private val CHARS_SWITCH = "switch".toCharArray()
        private val CHARS_THIS = "this".toCharArray()
        private const val CHARS_GENERATOR = '*'

        private val CHARS_SUPER = "super".toCharArray()
        private val CHARS_THROW = "throw".toCharArray()
        private val CHARS_TRUE = "true".toCharArray()
        private val CHARS_TRY = "try".toCharArray()
        private val CHARS_VAR = "var".toCharArray()
        private val CHARS_LET = "let".toCharArray()
        private val CHARS_CONST = "const".toCharArray()
        private val CHARS_WHILE = "while".toCharArray()
        private val CHARS_ELLIPSIS = "...".toCharArray()
        private val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

        private val COMMON_ESCAPE_MAPPING = mapOf(
            '\b' to 'b'.code,
            '\u000c' to 'f'.code,
            '\n' to 'n'.code,
            '\r' to 'r'.code,
            '\t' to 't'.code,
            '\\' to '\\'.code
        )

        private val STRING_ESCAPE_MAPPING = COMMON_ESCAPE_MAPPING

        private val TEMPLATE_ESCAPE_MAPPING = STRING_ESCAPE_MAPPING + mapOf(
            '$' to '$'.code
        )

        /**
         * Generate JavaScript code that evaluates to the supplied string. Adapted
         * from `org.mozilla.javascript.ScriptRuntime#escapeString(String)`.
         *
         * The difference is that we quote with either `&quot;` or `&apos;` depending on
         * which one is used less inside the string.
         */
        fun javaScriptString(str: String, forceDoubleQuote: Boolean = false): String {
            var quoteCount = 0
            var aposCount = 0

            for (char in str) {
                when (char) {
                    '"' -> quoteCount++
                    '\'' -> aposCount++
                }
            }

            val quoteChar = if (quoteCount < aposCount || forceDoubleQuote) '"' else '\''

            return buildString(str.length + 16) {
                append(quoteChar)
                appendEscapedString(str, quoteChar, STRING_ESCAPE_MAPPING)
                append(quoteChar)
                appendEscapeClosingTags()
            }
        }

        private fun StringBuilder.appendEscapedString(str: String, quoteChar: Char, escapeMapping: Map<Char, Int>) {
            for (char in str) {
                if (char == quoteChar) {
                    append('\\')
                    append(quoteChar)
                    continue
                }

                if (char in ' '..'~' && char != '\\' && !escapeMapping.containsKey(char)) {
                    // an ordinary print character (like C isprint())
                    append(char)
                    continue
                }

                val escape: Int? = escapeMapping.get(char)

                if (escape != null && escape >= 0) {
                    // an \escaped sort of character
                    append('\\')
                    append(escape.toChar())
                    continue
                }

                val hexSize: Int
                if (char.code < 256) {
                    append("\\x")
                    hexSize = 2
                } else {
                    append("\\u")
                    hexSize = 4
                }

                // append hexadecimal form of ch left-padded with 0
                var shift = (hexSize - 1) * 4
                while (shift >= 0) {
                    val digit = 0xf and (char.code shr shift)
                    append(HEX_DIGITS[digit])
                    shift -= 4
                }
            }
        }

        private fun escapeTemplateStringSegment(str: String): String {
            return buildString {
                appendEscapedString(str, '`', TEMPLATE_ESCAPE_MAPPING)
                appendEscapeClosingTags()
            }
        }

        /**
         * Escapes any closing XML tags embedded in `str`, which could
         * potentially cause a parse failure in a browser, for example, embedding a
         * closing `<script>` tag.
         */
        private fun StringBuilder.appendEscapeClosingTags() {
            var index = 0
            while ((indexOf("</", index).also { index = it }) != -1) {
                insert(index + 1, '\\')
            }
        }
    }

    private val sourceInfoStack = mutableListOf<JsLocationWithSource?>()

    protected var insideComments: Boolean = false
    protected var needSemi: Boolean = true
    private var lineBreakAfterBlock = true

    /**
     * "Global" blocks are either the global block of a fragment, or a block
     * nested directly within some other global block. This definition matters
     * because the statements designated by statementEnds and statementStarts are
     * those that appear directly within these global blocks.
     */
    private val globalBlocks = mutableSetOf<JsBlock>()

    override fun visitArrayAccess(x: JsArrayAccess) {
        printCommentsBeforeNode(x)
        pushSourceInfo(x.source)

        printPair(x, x.arrayExpression)
        leftSquare()
        accept(x.indexExpression)
        rightSquare()

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitArray(x: JsArrayLiteral) {
        printCommentsBeforeNode(x)
        pushSourceInfo(x.source)

        leftSquare()
        printExpressions(x.expressions)
        rightSquare()

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    private fun printExpressions(expressions: List<JsExpression>) {
        var notFirst = false
        for (expression in expressions) {
            notFirst = sepCommaSpace(notFirst) && expression !is JsDocComment
            val isEnclosed = parenPushIfCommaExpression(expression)
            accept(expression)
            if (isEnclosed) {
                rightParen()
            }
        }
    }

    override fun visitBinaryExpression(x: JsBinaryOperation) {
        printCommentsBeforeNode(x)
        pushSourceInfo(x.source)

        val isExpressionEnclosed = parenPush(x, x.arg1, !x.operator.isLeftAssociative)

        accept(x.arg1)

        if (x.operator.isKeyword) {
            parenPopOrSpace(x, x.arg1, !x.operator.isLeftAssociative)
        } else if (x.operator != JsBinaryOperator.COMMA) {
            if (isExpressionEnclosed) {
                rightParen()
            }
            space()
        }

        p.print(x.operator.symbol)

        val arg2 = x.arg2
        val isParenOpened =
            if (x.operator == JsBinaryOperator.COMMA) {
                space()
                false
            } else if (arg2 is JsBinaryOperation && arg2.operator == JsBinaryOperator.AND) {
                space()
                leftParen()
                true
            } else if (arg2 == null) {
                space()
                false
            } else {
                if (spaceCalc(x.operator, arg2)) {
                    parenPushOrSpace(x, arg2, x.operator.isLeftAssociative)
                } else {
                    space()
                    parenPush(x, arg2, x.operator.isLeftAssociative)
                }
            }

        accept(arg2)

        if (isParenOpened) {
            rightParen()
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitSimpleAssignment(x: JsAssignmentOperation.Simple) {
        printCommentsBeforeNode(x)
        pushSourceInfo(x.source)

        // Assignment is right-associative, so the left-hand side is parenthesized only when it has
        // strictly lower precedence (wrongAssoc), matching the former JsBinaryOperator.ASG rendering.
        val isTargetEnclosed = parenPush(x, x.target, true)

        accept(x.target)

        if (isTargetEnclosed) {
            rightParen()
        }
        space()
        assignment()

        val value = x.value
        val isValueEnclosed =
            if (value is JsBinaryOperation && value.operator == JsBinaryOperator.AND) {
                space()
                leftParen()
                true
            } else {
                space()
                parenPush(x, value, false)
            }

        accept(value)

        if (isValueEnclosed) {
            rightParen()
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitDestructuringAssignment(x: JsAssignmentOperation.Destructuring) {
        printCommentsBeforeNode(x)
        pushSourceInfo(x.source)

        x.pattern.accept(this)
        space()
        assignment()
        space()
        accept(x.value)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitBlock(x: JsBlock) {
        printJsBlock(x, true, null)
    }

    override fun visitBoolean(x: JsBooleanLiteral) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        if (x.value) {
            p.print(CHARS_TRUE)
        } else {
            p.print(CHARS_FALSE)
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitBreak(x: JsBreak) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_BREAK)
        continueOrBreakLabel(x)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitContinue(x: JsContinue) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_CONTINUE)
        continueOrBreakLabel(x)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitYield(x: JsYield) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_YIELD)

        if (x.expression != null) {
            space()
            accept(x.expression)
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitYieldStar(x: JsYieldStar) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_YIELD_STAR)

        if (x.expression != null) {
            space()
            accept(x.expression)
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitSpread(spread: JsSpread) {
        pushSourceInfo(spread.source)
        printCommentsBeforeNode(spread)

        ellipsis()
        printPair(spread, spread.expression)

        printCommentsAfterNode(spread)
        popSourceInfo()
    }

    private fun continueOrBreakLabel(x: JsContinue) {
        x.label?.let {
            space()
            p.print(it.ident)
        }
    }

    override fun visitCase(x: JsCase) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_CASE)
        space()
        accept(x.caseExpression)
        colon()

        printCommentsAfterNode(x)
        popSourceInfo()

        newline()

        sourceLocationConsumer.pushSourceInfo(null)
        printSwitchMemberStatements(x)
        sourceLocationConsumer.popSourceInfo()
    }

    private fun printSwitchMemberStatements(x: JsSwitchMember) {
        p.indentIn()
        for (stmt in x.statements) {
            needSemi = true
            accept(stmt)
            if (needSemi) {
                semi()
            }
            newline()
        }
        p.indentOut()
        needSemi = false
    }

    override fun visitCatch(x: JsCatch) {
        printCommentsBeforeNode(x)
        pushSourceInfo(x.source)

        space()
        p.print(CHARS_CATCH)
        space()

        leftParen()
        accept(x.parameter.declarable)
        rightParen()

        space()

        popSourceInfo()
        printCommentsAfterNode(x)

        sourceLocationConsumer.pushSourceInfo(null)
        accept(x.body)
        sourceLocationConsumer.popSourceInfo()
    }

    override fun visitConditional(x: JsConditional) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        // Associativity: for the then and else branches, it is safe to insert
        // another
        // ternary expression, but if the test expression is a ternary, it should
        // get parentheses around it.
        printPair(x, x.testExpression, true)
        space()

        p.print('?')
        space()

        printPair(x, x.thenExpression)
        space()

        colon()
        space()

        printPair(x, x.elseExpression)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    private fun printPair(parent: JsExpression, expression: JsExpression, wrongAssoc: Boolean) {
        val isNeedParen = parenCalc(parent, expression, wrongAssoc)
        if (isNeedParen) {
            leftParen()
        }
        accept(expression)
        if (isNeedParen) {
            rightParen()
        }
    }

    private fun printPair(parent: JsExpression, expression: JsExpression) {
        printPair(parent, expression, false)
    }

    override fun visitDebugger(x: JsDebugger) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_DEBUGGER)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitDefault(x: JsDefault) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_DEFAULT)
        colon()

        printCommentsAfterNode(x)
        popSourceInfo()

        newline()

        sourceLocationConsumer.pushSourceInfo(null)
        printSwitchMemberStatements(x)
        sourceLocationConsumer.popSourceInfo()
    }

    override fun visitWhile(x: JsWhile) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        _while()
        space()

        leftParen()
        accept(x.condition)
        rightParen()

        printCommentsAfterNode(x)
        popSourceInfo()

        val body = materialize(x.body)

        nestedPush(body)
        sourceLocationConsumer.pushSourceInfo(null)
        accept(body)
        sourceLocationConsumer.popSourceInfo()
        nestedPop(body)
    }

    override fun visitDoWhile(x: JsDoWhile) {
        sourceLocationConsumer.pushSourceInfo(null)
        printCommentsBeforeNode(x)

        p.print(CHARS_DO)

        val body = materialize(x.body)

        nestedPush(body)
        accept(body)
        sourceLocationConsumer.popSourceInfo()
        nestedPop(body)

        pushSourceInfo(x.condition.source)
        if (needSemi) {
            semi()
            newline()
        } else {
            space()
            needSemi = true
        }

        _while()
        space()

        leftParen()
        accept(x.condition)
        rightParen()

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitEmpty(x: JsEmpty) {}

    override fun visitExpressionStatement(x: JsExpressionStatement) {
        val source = when (x.expression) {
            !is JsFunction if x.source == null -> x.expression.source
            else -> x.source
        }

        pushSourceInfo(source)
        printCommentsBeforeNode(x)

        val surroundWithParentheses = JsFirstExpressionVisitor.exec(x)
        if (surroundWithParentheses) {
            leftParen()
        }
        accept(x.expression)
        if (surroundWithParentheses) {
            rightParen()
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitFor(x: JsFor) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        _for()
        space()
        leftParen()

        // The init expressions or var decl. Only one of them may be present at a time.
        when {
            x.initExpression != null -> accept(x.initExpression)
            x.initVars != null -> accept(x.initVars)
        }

        semi()

        // The loop test.
        x.condition?.let {
            space()
            accept(it)
        }

        semi()

        // The incr expression.
        x.incrementExpression?.let {
            space()
            accept(it)
        }

        rightParen()

        printCommentsAfterNode(x)
        popSourceInfo()

        val body = materialize(x.body)

        nestedPush(body)
        // Unlike the other loops, a `for` may have no body at all, e.g. `for (;;);`.
        if (body != null) {
            sourceLocationConsumer.pushSourceInfo(null)
            accept(body)
            sourceLocationConsumer.popSourceInfo()
        }
        nestedPop(body)
    }

    override fun visitForIn(x: JsForIn) {
        printIterableLoop(x, CHARS_IN)
    }

    override fun visitForOf(x: JsForOf) {
        printIterableLoop(x, CHARS_OF)
    }

    private fun printIterableLoop(x: JsIterableLoop, separatorChars: CharArray) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        _for()
        space()
        leftParen()

        if (x.bindingDeclarable != null && x.bindingVarVariant != null) {
            varModifier(x.bindingVarVariant)
            space()
            accept(x.bindingDeclarable)

            if (x.bindingExpression != null) {
                space()
                assignment()
                space()
                accept(x.bindingExpression)
            }
        } else {
            // Just a name ref.
            //
            accept(x.bindingExpression)
        }

        space()
        p.print(separatorChars)
        space()
        accept(x.iterableExpression)

        rightParen()

        printCommentsAfterNode(x)
        popSourceInfo()

        val body = materialize(x.body)

        nestedPush(body)
        sourceLocationConsumer.pushSourceInfo(null)
        accept(body)
        sourceLocationConsumer.popSourceInfo()
        nestedPop(body)
    }

    override fun visitFunction(x: JsFunction) {
        printCommentsBeforeNode(x)

        when {
            x.isEs6Arrow -> printEs6Arrow(x)
            else -> printRegularFunction(x)
        }

        printCommentsAfterNode(x)
    }

    private fun printFunctionParameterList(parameters: List<JsParameter>) {
        leftParen()

        sourceLocationConsumer.pushSourceInfo(null)
        var notFirst = false
        for (param in parameters) {
            notFirst = sepCommaSpace(notFirst)
            printCommentsBeforeNode(param)
            accept(param)
            printCommentsAfterNode(param)
        }
        sourceLocationConsumer.popSourceInfo()

        rightParen()
    }

    // function <declaration>
    private fun printRegularFunction(x: JsFunction) {
        pushDeclaration(x)
        pushSourceInfo(x.source)

        p.print(CHARS_FUNCTION)
        space()
        printFunction(x)

        popSourceInfo()
        popDeclaration()
    }

    // constructor <declaration>
    private fun printConstructor(x: JsFunction) {
        pushDeclaration(x)
        pushSourceInfo(x.source)

        p.print(CHARS_CONSTRUCTOR)
        printFunction(x)

        popSourceInfo()
        popDeclaration()
    }

    // [static?] [get|set?] <declaration>
    private fun printClassMember(x: JsFunction) {
        pushDeclaration(x)
        pushSourceInfo(x.source)

        if (x.isStatic) {
            p.print(CHARS_STATIC)
            space()
        }

        when {
            x.isGetter -> {
                p.print(CHARS_GET)
                space()
            }
            x.isSetter -> {
                p.print(CHARS_SET)
                space()
            }
        }

        printFunction(x)

        popSourceInfo()
        popDeclaration()
    }

    // [name|computedName](<params>) { <body> }
    private fun printFunction(x: JsFunction) {
        if (x.isGenerator)
            p.print(CHARS_GENERATOR)

        when {
            x.computedName != null -> {
                leftSquare()
                accept(x.computedName)
                rightSquare()
            }
            x.name != null -> nameOf(x)
        }

        printFunctionParameterList(x.parameters)
        space()

        lineBreakAfterBlock = false

        sourceLocationConsumer.pushSourceInfo(null)
        printJsBlock(x.body, true, x.body.source)
        sourceLocationConsumer.popSourceInfo()

        needSemi = true
    }

    // (<params>) => { <body> }
    private fun printEs6Arrow(x: JsFunction) {
        pushSourceInfo(x.source)

        printFunctionParameterList(x.parameters)
        space()
        arrow()
        space()
        val body = x.body
        when (val firstStatement = body.statements.getOrNull(0)) {
            is JsReturn if body.statements.size == 1 ->
                firstStatement.expression.accept(this)
            else -> {
                lineBreakAfterBlock = false
                sourceLocationConsumer.pushSourceInfo(null)
                printJsBlock(body, true, x.body.source)
                sourceLocationConsumer.popSourceInfo()
            }
        }

        popSourceInfo()

        needSemi = true
    }

    override fun visitClass(x: JsClass) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_CLASS)
        if (x.name != null) {
            space()
            nameOf(x)
        }

        if (x.baseClass != null) {
            space()
            p.print(CHARS_EXTENDS)
            space()
            accept(x.baseClass)
        }

        space()

        if (x.constructor == null && x.members.isEmpty()) {
            p.print("{}")
            newline()
        } else {
            blockOpen()

            x.constructor?.let {
                it.name = null
                printConstructor(it)
                newline()
            }

            for (m in x.members) {
                printClassMember(m)
                newline()
            }

            blockClose()
        }
        needSemi = false

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitIf(x: JsIf) {
        printCommentsBeforeNode(x)
        pushSourceInfo(x.source)

        _if()
        space()

        leftParen()
        accept(x.ifExpression)
        rightParen()

        popSourceInfo()
        printCommentsAfterNode(x)

        var thenStmt = x.thenStatement
        val elseStatement = x.elseStatement

        // Wrap statements inside <then> branch with `{}` if this `if` has its own else,
        // but has at least 1 `if` without else inside chain of `if` statements.
        // This is done to prevent this if's else to accidentally attach to some inner `if` down the tree.
        if (elseStatement != null && isIfWithoutElse(thenStmt)) {
            thenStmt = JsBlock(thenStmt)
        }

        nestedPush(thenStmt)

        if (thenStmt is JsBlock && elseStatement != null) {
            lineBreakAfterBlock = false
        }

        sourceLocationConsumer.pushSourceInfo(null)
        accept(materialize(thenStmt))
        sourceLocationConsumer.popSourceInfo()

        nestedPop(thenStmt)

        if (elseStatement != null) {
            if (needSemi) {
                semi()
                newline()
            } else {
                space()
                needSemi = true
            }
            p.print(CHARS_ELSE)

            if (elseStatement !is JsIf) {
                nestedPush(elseStatement)
            } else {
                space()
            }

            sourceLocationConsumer.pushSourceInfo(null)
            accept(materialize(elseStatement))
            sourceLocationConsumer.popSourceInfo()

            if (elseStatement !is JsIf) {
                nestedPop(elseStatement)
            }
        }
    }

    private fun isIfWithoutElse(statement: JsStatement): Boolean {
        var statement: JsStatement? = statement
        while (statement is JsIf) {
            if (statement.elseStatement == null) {
                return true
            }
            statement = statement.elseStatement
        }

        return false
    }

    private fun materialize(statement: JsStatement?): JsStatement? {
        return when (statement) {
            is JsCompositeBlock -> JsBlock(statement)
            else -> statement
        }
    }

    override fun visitInvocation(invocation: JsInvocation) {
        pushSourceInfo(invocation.source)
        printCommentsBeforeNode(invocation)

        printPair(invocation, invocation.qualifier)

        leftParen()
        printExpressions(invocation.arguments)
        rightParen()

        printCommentsAfterNode(invocation)
        popSourceInfo()
    }

    override fun visitLabel(x: JsLabel) {
        nameOf(x)
        colon()
        space()

        sourceLocationConsumer.pushSourceInfo(null)
        accept(x.statement)
        sourceLocationConsumer.popSourceInfo()
    }

    override fun visitNameRef(nameRef: JsNameRef) {
        printCommentsBeforeNode(nameRef)
        p.maybeIndent()

        val qualifier = nameRef.qualifier
        if (qualifier != null) {
            val enclose = when (qualifier) {
                // "42.foo" is not allowed, but "(42).foo" is. A BigInt literal needs no parens:
                // the `n` suffix terminates it, so `1n.foo` already parses as a member access.
                is JsIntLiteral, is JsDoubleLiteral -> true
                else -> parenCalc(nameRef, qualifier, false)
            }

            if (enclose) leftParen()
            accept(qualifier)
            if (enclose) rightParen()

            p.print('.')
        }

        pushSourceInfo(nameRef.source)
        p.print(nameRef.ident)
        popSourceInfo()

        printCommentsAfterNode(nameRef)
    }

    override fun visitNew(x: JsNew) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_NEW)
        space()

        val needsParens = JsConstructExpressionVisitor.exec(x.constructorExpression)
        if (needsParens) leftParen()
        accept(x.constructorExpression)
        if (needsParens) rightParen()

        leftParen()
        printExpressions(x.arguments)
        rightParen()

        // When using class expressions as construction expressions, they reset this from default 'true' to 'false',
        // which produces invalid code due to the next statements ambiguity.
        needSemi = true

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitNull(x: JsNullLiteral) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_NULL)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitInt(x: JsIntLiteral) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(x.value)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitDouble(x: JsDoubleLiteral) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(x.value)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitBigInt(x: JsBigIntLiteral) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(x.value.toString())
        p.print('n')

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitObjectLiteral(x: JsObjectLiteral) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print('{')

        if (x.isMultiline) {
            p.indentIn()
        }

        var notFirst = false
        for (item in x.propertyInitializers) {
            if (notFirst) {
                p.print(',')
            }

            when {
                x.isMultiline -> newline()
                notFirst -> space()
            }

            notFirst = true

            pushSourceInfo(item.source)

            when (item) {
                is JsPropertyInitializer.Spread -> {
                    ellipsis()
                    accept(item.expression)
                }
                is JsPropertyInitializer.KeyValue -> {
                    when (val labelExpr = item.labelExpr) {
                        is JsStringLiteral -> {
                            val value = labelExpr.value
                            if (value.isValidES5Identifier()) {
                                val escaped = if (RESERVED_KEYWORDS.contains(value)) "'$value'" else value
                                accept(JsNameRef(escaped).withMetadataFrom(labelExpr))
                            } else
                                accept(labelExpr)
                        }
                        is JsNumberLiteral -> accept(labelExpr)
                        else -> {
                            leftSquare()
                            accept(labelExpr)
                            rightSquare()
                        }
                    }

                    colon()
                    space()

                    val wasEnclosed = parenPushIfCommaExpression(item.valueExpr)
                    accept(item.valueExpr)
                    if (wasEnclosed) rightParen()
                }
            }

            popSourceInfo()
        }

        if (x.isMultiline) {
            p.indentOut()
            newline()
        }

        p.print('}')

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitParameter(x: JsParameter) {
        pushSourceInfo(x.source)

        if (x.isRest) ellipsis()

        accept(x.declarable)

        x.defaultValue?.let {
            space()
            assignment()
            space()

            val wasEnclosed = parenPushIfCommaExpression(it)
            accept(it)
            if (wasEnclosed) {
                rightParen()
            }
        }

        popSourceInfo()
    }

    override fun visitPostfixOperation(x: JsPostfixOperation) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        // unary operators always associate correctly (I think)
        printPair(x, x.arg)
        p.print(x.operator.symbol)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitPrefixOperation(x: JsPrefixOperation) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(x.operator.symbol)

        if (spaceCalc(x.operator, x.arg)) {
            space()
        }

        // unary operators always associate correctly (I think)
        printPair(x, x.arg)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitProgram(x: JsProgram) {
        x.acceptChildren(this)
    }

    override fun visitRegExp(x: JsRegExp) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        slash()
        p.print(x.pattern)
        slash()

        x.flags?.let {
            p.print(it)
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitReturn(x: JsReturn) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_RETURN)
        x.expression?.let {
            space()
            accept(it)
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitString(x: JsStringLiteral) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(javaScriptString(x.value))

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitTemplateString(x: JsTemplateStringLiteral) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        accept(x.tag)

        p.print('`')
        for (segment in x.segments) {
            accept(segment)
        }
        p.print('`')

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitTemplateSegmentString(x: JsTemplateStringLiteral.Segment.StringLiteral) {
        pushSourceInfo(x.source)

        p.print(escapeTemplateStringSegment(x.value))

        popSourceInfo()
    }

    override fun visitTemplateSegmentInterpolation(x: JsTemplateStringLiteral.Segment.Interpolation) {
        pushSourceInfo(x.source)

        p.print($$"${")
        accept(x.expression)
        p.print('}')

        popSourceInfo()
    }

    override fun visit(x: JsSwitch) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_SWITCH)
        space()

        leftParen()
        accept(x.expression)
        rightParen()

        printCommentsAfterNode(x)
        popSourceInfo()

        sourceLocationConsumer.pushSourceInfo(null)
        space()

        blockOpen()
        acceptList(x.cases)
        blockClose()
        sourceLocationConsumer.popSourceInfo()
    }

    override fun visitThis(x: JsThisRef) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_THIS)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitSuper(x: JsSuperRef) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_SUPER)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitThrow(x: JsThrow) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        p.print(CHARS_THROW)
        space()

        accept(x.expression)

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitTry(x: JsTry) {
        printCommentsBeforeNode(x)
        pushSourceInfo(x.source)

        p.print(CHARS_TRY)
        space()
        lineBreakAfterBlock = false

        popSourceInfo()

        accept(x.tryBlock)
        acceptList(x.catches)

        val finallyBlock = x.finallyBlock
        if (finallyBlock != null) {
            p.print(CHARS_FINALLY)
            space()

            accept(finallyBlock)
        }

        printCommentsAfterNode(x)
    }

    override fun visit(x: JsVars.JsVar) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        accept(x.declarable)
        x.initExpression?.let {
            space()
            assignment()
            space()

            val isEnclosed = parenPushIfCommaExpression(it)
            accept(it)
            if (isEnclosed) {
                rightParen()
            }
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitVars(x: JsVars) {
        pushSourceInfo(x.source)
        printCommentsBeforeNode(x)

        varModifier(x.variant)
        space()

        var notFirst = false
        for (`var` in x) {
            if (notFirst) {
                if (x.isMultiline) newline()
                p.print(',')
                space()
            } else {
                notFirst = true
            }

            accept(`var`)
        }

        printCommentsAfterNode(x)
        popSourceInfo()
    }

    override fun visitSingleLineComment(comment: JsSingleLineComment) {
        if (needSemi && insideComments) {
            semi()
            space()
        }
        p.print("//")
        p.print(comment.text)
        newline()
        needSemi = false
    }

    override fun visitMultiLineComment(comment: JsMultiLineComment) {
        val lines = comment.text.lines()

        p.print("/*")
        p.print(lines[0].trim { it <= ' ' })

        for (i in 1..<lines.size) {
            newline()
            p.print(lines[i].trim { it <= ' ' })
        }

        p.print("*/")
    }

    override fun visitDocComment(comment: JsDocComment) {
        val asSingleLine = comment.tags.size == 1
        if (!asSingleLine) newline()

        p.print("/**")

        if (asSingleLine) {
            space()
        } else {
            newline()
        }

        var notFirst = false
        for ((key, value) in comment.tags) {
            if (notFirst) {
                newline()
                p.print(" *")
            } else {
                notFirst = true
            }

            p.print("@$key")

            if (value != null) {
                space()

                when (value) {
                    is CharSequence -> p.print(value)
                    is JsNameRef -> visitNameRef(value)
                    else -> error("Unsupported type of the '$key' doc comment tag value: ${value.javaClass}")
                }
            }

            if (!asSingleLine) {
                newline()
            }
        }

        if (asSingleLine) {
            space()
        } else {
            newline()
        }

        p.print("*/")

        if (asSingleLine) {
            space()
        }
    }

    override fun visitExport(export: JsExport) {
        p.print("export")
        space()

        when (val subject = export.subject) {
            is JsExport.Subject.All -> {
                p.print("*")
            }
            is JsExport.Subject.Default -> {
                p.print("default")
                space()
                visitNameRef(subject.name)
            }
            is JsExport.Subject.Elements -> {
                blockOpen()
                for (element in subject.elements) {
                    visitNameRef(element.name)
                    val alias = element.alias
                    if (alias != null) {
                        p.print(" as ")
                        if (alias.ident.isValidES5Identifier()) {
                            nameDef(alias)
                        } else {
                            p.print(javaScriptString(alias.ident))
                        }
                    }
                    p.print(',')
                    p.newline()
                }
                p.indentOut()
                p.print('}')
            }
        }

        if (export.fromModule != null) {
            p.print(" from ")
            p.print(javaScriptString(export.fromModule))
        }

        needSemi = true
    }

    override fun visitImport(import: JsImport) {
        val target = import.target

        p.print("import ")

        when (target) {
            is JsImport.Target.Default -> {
                visitNameRef(target.name)
            }
            is JsImport.Target.All -> {
                p.print("* as ")
                visitNameRef(target.alias)
            }
            is JsImport.Target.Elements -> {
                val elements = target.elements

                p.print("{")
                val isMultiline = elements.size > 1
                p.indentIn()

                if (isMultiline) newline()
                else space()

                for (element in elements) {
                    val importedName = element.name

                    if (importedName.ident.isValidES5Identifier()) {
                        nameDef(importedName)
                    } else {
                        p.print(javaScriptString(importedName.ident))
                    }

                    val alias = element.alias
                    if (alias != null) {
                        p.print(" as ")
                        visitNameRef(alias)
                    }

                    if (isMultiline) {
                        p.print(',')
                        newline()
                    } else {
                        space()
                    }
                }

                p.indentOut()
                p.print("}")
            }
            else -> {}
        }

        if (target !is JsImport.Target.Effect)
            p.print(" from ")

        p.print(javaScriptString(import.module))
    }

    override fun visitNamedDeclarable(declarable: JsDeclarable.Named) {
        nameDef(declarable.name)
    }

    override fun visitArrayPatternDeclarable(pattern: JsDeclarable.ArrayPattern) {
        pushSourceInfo(pattern.source)
        printCommentsBeforeNode(pattern)

        p.print('[')

        var notFirst = false
        for (item in pattern.elements) {
            notFirst = sepCommaSpace(notFirst)

            val element = when (item) {
                is JsBindingArrayItem.Hole -> continue
                is JsBindingArrayItem.Element -> item.element
            }
            visitBindingElement(element)
        }

        p.print(']')

        printCommentsAfterNode(pattern)
        popSourceInfo()
    }

    override fun visitObjectPatternDeclarable(pattern: JsDeclarable.ObjectPattern) {
        pushSourceInfo(pattern.source)
        printCommentsBeforeNode(pattern)

        p.print('{')
        var notFirst = false
        for (property in pattern.properties) {
            notFirst = sepCommaSpace(notFirst)

            val propertyName = property.propertyName
            val element = property.element

            if (propertyName != null) {
                propertyName.accept(this)
                colon()
                space()
            }

            visitBindingElement(element)
        }
        p.print('}')

        printCommentsAfterNode(pattern)
        popSourceInfo()
    }

    override fun visitBindingElement(element: JsBindingElement) {
        if (element.isSpread) {
            ellipsis()
        }

        element.target.accept(this)
        element.defaultValue?.let {
            space()
            assignment()

            space()

            accept(it)
        }
    }

    private fun newline() {
        p.newline()
        sourceLocationConsumer.newLine()
    }

    private fun pushSourceInfo(location: JsLocationWithSource?) {
        p.maybeIndent()
        sourceInfoStack.add(location)
        if (location != null) {
            sourceLocationConsumer.pushSourceInfo(location)
        }
    }

    private fun pushDeclaration(declaration: JsFunction) {
        sourceLocationConsumer.pushDeclarationInfo(declaration.source)
    }

    private fun printCommentsBeforeNode(x: JsNode) {
        printComments(x.commentsBeforeNode, false)
    }

    private fun printCommentsAfterNode(x: JsNode) {
        printComments(x.commentsAfterNode, true)
    }

    private fun printComments(comments: List<JsComment>?, isAfterNode: Boolean) {
        if (comments == null) return

        val previousNeedSemi = needSemi
        needSemi = isAfterNode
        insideComments = true

        for (comment in comments) {
            comment.accept(this)
        }

        insideComments = false

        if (!isAfterNode) {
            needSemi = previousNeedSemi
        }
    }

    private fun popSourceInfo() {
        if (sourceInfoStack.isNotEmpty() && sourceInfoStack.removeLast() != null) {
            sourceLocationConsumer.popSourceInfo()
        }
    }

    private fun popDeclaration() {
        sourceLocationConsumer.popDeclarationInfo()
    }

    private fun printJsBlock(x: JsBlock, finalNewline: Boolean, defaultClosingBraceLocation: JsLocationWithSource?) {
        var finalNewline = finalNewline
        if (!lineBreakAfterBlock) {
            finalNewline = false
            lineBreakAfterBlock = true
        }

        printCommentsBeforeNode(x)

        val needBraces = !x.isTransparent

        if (needBraces) {
            sourceLocationConsumer.pushSourceInfo(x.source)
            blockOpen()
            sourceLocationConsumer.popSourceInfo()
        }

        sourceLocationConsumer.pushSourceInfo(null)

        val iterator = x.statements.iterator()
        while (iterator.hasNext()) {
            val isGlobal = x.isTransparent || globalBlocks.contains(x)

            val statement = iterator.next()
            if (statement is JsEmpty) {
                continue
            }

            needSemi = true
            var stmtIsGlobalBlock = false
            if (isGlobal) {
                if (statement is JsBlock) {
                    // A block inside a global block is still considered global
                    stmtIsGlobalBlock = true
                    globalBlocks.add(statement)
                }
            }

            accept<JsStatement?>(statement)
            if (stmtIsGlobalBlock) {
                globalBlocks.remove(statement)
            }
            if (needSemi) {
                /*
                * Special treatment of function declarations: If they are the only item in a
                * statement (i.e. not part of an assignment operation), just give them
                * a newline instead of a semi.
                */
                val functionStmt =
                    statement is JsExpressionStatement && statement.expression is JsFunction
                /*
                * Special treatment of the last statement in a block: only a few
                * statements at the end of a block require semicolons.
                */
                val lastStatement = !iterator.hasNext() && needBraces && !JsRequiresSemiVisitor.exec(statement)
                if (functionStmt) {
                    newline()
                } else {
                    if (lastStatement) {
                        p.print(';')
                    } else {
                        semi()
                    }
                    newline()
                }
            }
        }

        if (needBraces) {
            // _blockClose() modified
            p.indentOut()

            sourceLocationConsumer.popSourceInfo()

            val closingBraceLocation = x.closingBraceSource ?: defaultClosingBraceLocation

            if (closingBraceLocation != null) {
                pushSourceInfo(closingBraceLocation)
            }
            p.print('}')
            if (closingBraceLocation != null) {
                popSourceInfo()
            }

            if (finalNewline) {
                newline()
            }
        } else {
            sourceLocationConsumer.popSourceInfo()
        }
        needSemi = false
        printCommentsAfterNode(x)
    }

    private fun assignment() {
        p.print('=')
    }

    private fun arrow() {
        p.print("=>")
    }

    private fun blockClose() {
        p.indentOut()
        p.print('}')
        newline()
    }

    private fun blockOpen() {
        p.print('{')
        p.indentIn()
        newline()
    }

    private fun colon() {
        p.print(':')
    }

    private fun _for() {
        p.print(CHARS_FOR)
    }

    private fun _if() {
        p.print(CHARS_IF)
    }

    private fun leftParen() {
        p.print('(')
    }

    private fun leftSquare() {
        p.print('[')
    }

    private fun nameDef(name: JsName) {
        p.print(name.ident)
    }

    private fun nameOf(hasName: HasName) {
        nameDef(hasName.name)
    }

    private fun nestedPop(statement: JsStatement?) {
        if (statement !is JsBlock) {
            p.indentOut()
        }
    }

    private fun nestedPush(statement: JsStatement?) {
        when (statement) {
            is JsBlock -> space()
            else -> {
                newline()
                p.indentIn()
            }
        }
    }

    /**
     * Calculates whether parenthesis are needed around the [child] node, which is inside the [parent] node.
     *
     * @return `true`, if left and right parens are required, otherwise `false`.
     */
    private fun parenCalc(parent: JsExpression, child: JsExpression, wrongAssoc: Boolean): Boolean {
        val parentPrecedence = JsPrecedenceVisitor.exec(parent)
        val childPrecedence = JsPrecedenceVisitor.exec(child)
        return parentPrecedence > childPrecedence || parentPrecedence == childPrecedence && wrongAssoc
    }

    private fun parenPopOrSpace(parent: JsExpression, child: JsExpression, wrongAssoc: Boolean) {
        if (parenCalc(parent, child, wrongAssoc)) {
            rightParen()
        } else {
            space()
        }
    }

    private fun parenPush(parent: JsExpression, child: JsExpression, wrongAssoc: Boolean): Boolean {
        return parenCalc(parent, child, wrongAssoc).also { doPush ->
            if (doPush) leftParen()
        }
    }

    private fun parenPushIfCommaExpression(x: JsExpression?): Boolean {
        return (x is JsBinaryOperation && x.operator == JsBinaryOperator.COMMA).also { doPush ->
            if (doPush) leftParen()
        }
    }

    private fun parenPushOrSpace(parent: JsExpression, child: JsExpression, wrongAssoc: Boolean): Boolean {
        return parenCalc(parent, child, wrongAssoc).also { doPush ->
            if (doPush) leftParen()
            else space()
        }
    }

    private fun rightParen() {
        p.print(')')
    }

    private fun rightSquare() {
        p.print(']')
    }

    private fun semi() {
        p.print(';')
    }

    private fun sepCommaSpace(isNonFirst: Boolean): Boolean {
        if (isNonFirst) {
            p.print(',')
            space()
        }
        return true
    }

    private fun slash() {
        p.print('/')
    }

    private fun space() {
        p.print(' ')
    }

    /**
     * Decide whether, if `op` is printed followed by `arg`,
     * there needs to be a space between the operator and expression.
     *
     * @return `true` if a space needs to be printed
     */
    private fun spaceCalc(op: JsOperator, arg: JsExpression?): Boolean {
        return op.isKeyword || when (arg) {
            is JsBinaryOperation ->
                // If the binary operation has a higher precedence than op, then it won't be parenthesized,
                // so check the first argument of the binary operation.
                arg.operator.precedence > op.precedence && spaceCalc(op, arg.arg1)
            is JsPrefixOperation -> {
                val prefixOp = arg.operator
                (op == JsBinaryOperator.SUB || op == JsUnaryOperator.NEG)
                        && (prefixOp == JsUnaryOperator.DEC || prefixOp == JsUnaryOperator.NEG)
                        || (op == JsBinaryOperator.ADD && prefixOp == JsUnaryOperator.INC)
            }
            is JsNumberLiteral if (op == JsBinaryOperator.SUB || op == JsUnaryOperator.NEG) -> {
                when (arg) {
                    is JsIntLiteral -> arg.value < 0
                    is JsBigIntLiteral -> arg.value < BigInteger.ZERO
                    is JsDoubleLiteral -> arg.value < 0
                    else -> error("spaceCalc numeric argument type is not supported: ${arg.javaClass}")
                }
            }
            else -> false
        }
    }

    private fun varModifier(variant: JsVars.Variant) {
        when (variant) {
            JsVars.Variant.Var -> _var()
            JsVars.Variant.Let -> let()
            JsVars.Variant.Const -> _const()
        }
    }

    private fun _var() {
        p.print(CHARS_VAR)
    }

    private fun let() {
        p.print(CHARS_LET)
    }

    private fun _const() {
        p.print(CHARS_CONST)
    }

    private fun _while() {
        p.print(CHARS_WHILE)
    }

    private fun ellipsis() {
        p.print(CHARS_ELLIPSIS)
    }
}
