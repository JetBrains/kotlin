/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.js.util.TextOutput
import java.math.BigInteger

open class JsToStringGenerationVisitor(
    protected val p: TextOutput,
    private val sourceLocationConsumer: SourceLocationConsumer = NoOpSourceLocationConsumer,
) : JsVisitor() {
    private val sourceInfoStack = mutableListOf<JsLocationWithSource?>()
    private val state = GenerationState()

    override fun visitArrayAccess(x: JsArrayAccess) {
        // Leading comments are printed outside of the source mapping scope, trailing ones inside of it.
        // This asymmetry is inherited from the Java implementation and is kept to produce identical source maps.
        printCommentsBeforeNode(x)
        withSourceMapping(x.source) {
            printPair(x, x.arrayExpression)
            leftSquare()
            accept(x.indexExpression)
            rightSquare()

            printCommentsAfterNode(x)
        }
    }

    override fun visitArray(x: JsArrayLiteral) {
        printCommentsBeforeNode(x)
        withSourceMapping(x.source) {
            leftSquare()
            printExpressions(x.expressions)
            rightSquare()

            printCommentsAfterNode(x)
        }
    }

    override fun visitBinaryExpression(x: JsBinaryOperation) {
        printCommentsBeforeNode(x)
        withSourceMapping(x.source) {
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
        }
    }

    override fun visitSimpleAssignment(x: JsAssignmentOperation.Simple) {
        printCommentsBeforeNode(x)
        withSourceMapping(x.source) {
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
        }
    }

    override fun visitDestructuringAssignment(x: JsAssignmentOperation.Destructuring) {
        printCommentsBeforeNode(x)
        withSourceMapping(x.source) {
            x.pattern.accept(this)
            space()
            assignment()
            space()
            accept(x.value)

            printCommentsAfterNode(x)
        }
    }

    override fun visitBlock(x: JsBlock) {
        printJsBlock(x, true, null)
    }

    override fun visitBoolean(x: JsBooleanLiteral) {
        withSourceMapping(x.source) {
            withComments(x) {
                if (x.value) {
                    p.print(Chars.TRUE)
                } else {
                    p.print(Chars.FALSE)
                }
            }
        }
    }

    override fun visitBreak(x: JsBreak) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.BREAK)
                continueOrBreakLabel(x)
            }
        }
    }

    override fun visitContinue(x: JsContinue) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.CONTINUE)
                continueOrBreakLabel(x)
            }
        }
    }

    override fun visitYield(x: JsYield) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.YIELD)

                if (x.expression != null) {
                    space()
                    accept(x.expression)
                }
            }
        }
    }

    override fun visitYieldStar(x: JsYieldStar) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.YIELD_STAR)

                if (x.expression != null) {
                    space()
                    accept(x.expression)
                }
            }
        }
    }

    override fun visitSpread(spread: JsSpread) {
        withSourceMapping(spread.source) {
            withComments(spread) {
                ellipsis()
                printPair(spread, spread.expression)
            }
        }
    }

    override fun visitCase(x: JsCase) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.CASE)
                space()
                accept(x.caseExpression)
                colon()
            }
        }

        newline()

        withEmptySourceMapping {
            printSwitchMemberStatements(x)
        }
    }

    override fun visitCatch(x: JsCatch) {
        withComments(x) {
            withSourceMapping(x.source) {
                space()
                p.print(Chars.CATCH)
                space()

                leftParen()
                accept(x.parameter.declarable)
                rightParen()

                space()
            }
        }

        withEmptySourceMapping {
            accept(x.body)
        }
    }

    override fun visitConditional(x: JsConditional) {
        withSourceMapping(x.source) {
            withComments(x) {
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
            }
        }
    }

    override fun visitDebugger(x: JsDebugger) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.DEBUGGER)
            }
        }
    }

    override fun visitDefault(x: JsDefault) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.DEFAULT)
                colon()
            }
        }

        newline()

        withEmptySourceMapping {
            printSwitchMemberStatements(x)
        }
    }

    override fun visitWhile(x: JsWhile) {
        withSourceMapping(x.source) {
            withComments(x) {
                _while()
                space()

                leftParen()
                accept(x.condition)
                rightParen()
            }
        }

        val body = materialize(x.body)
        withNestedIndent(body) {
            withEmptySourceMapping {
                accept(body)
            }
        }
    }

    override fun visitDoWhile(x: JsDoWhile) {
        val body = materialize(x.body)

        // The body is printed with no source mapping, and the closing `while (...)` is mapped to the condition.
        // Note that the indentation of the body is closed only after the mapping scope is left, as in the Java implementation.
        withEmptySourceMapping {
            printCommentsBeforeNode(x)

            p.print(Chars.DO)

            nestedPush(body)
            accept(body)
        }
        nestedPop(body)

        withSourceMapping(x.condition.source) {
            if (state.needsSemicolon) {
                semi()
                newline()
            } else {
                space()
                state.needsSemicolon = true
            }

            _while()
            space()

            leftParen()
            accept(x.condition)
            rightParen()

            printCommentsAfterNode(x)
        }
    }

    override fun visitEmpty(x: JsEmpty) {}

    override fun visitExpressionStatement(x: JsExpressionStatement) {
        val source = when (x.expression) {
            !is JsFunction if x.source == null -> x.expression.source
            else -> x.source
        }

        withSourceMapping(source) {
            withComments(x) {
                val withParentheses = JsFirstExpressionVisitor.exec(x)
                if (withParentheses) leftParen()
                accept(x.expression)
                if (withParentheses) rightParen()
            }
        }
    }

    override fun visitFor(x: JsFor) {
        withSourceMapping(x.source) {
            withComments(x) {
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
            }
        }

        val body = materialize(x.body)
        withNestedIndent(body) {
            // Unlike the other loops, a `for` may have no body at all, e.g. `for (;;);`.
            if (body != null) {
                withEmptySourceMapping {
                    accept(body)
                }
            }
        }
    }

    override fun visitForIn(x: JsForIn) {
        printIterableLoop(x, Chars.IN)
    }

    override fun visitForOf(x: JsForOf) {
        printIterableLoop(x, Chars.OF)
    }

    override fun visitFunction(x: JsFunction) {
        withComments(x) {
            when {
                x.isEs6Arrow -> printEs6Arrow(x)
                else -> printRegularFunction(x)
            }
        }
    }

    override fun visitClass(x: JsClass) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.CLASS)
                if (x.name != null) {
                    space()
                    nameOf(x)
                }

                if (x.baseClass != null) {
                    space()
                    p.print(Chars.EXTENDS)
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
                state.needsSemicolon = false
            }
        }
    }

    override fun visitIf(x: JsIf) {
        withComments(x) {
            withSourceMapping(x.source) {
                _if()
                space()

                leftParen()
                accept(x.ifExpression)
                rightParen()
            }
        }

        var thenStmt = x.thenStatement
        val elseStatement = x.elseStatement

        // Wrap statements inside <then> branch with `{}` if this `if` has its own else,
        // but has at least 1 `if` without else inside chain of `if` statements.
        // This is done to prevent this if's else to accidentally attach to some inner `if` down the tree.
        if (elseStatement != null && isIfWithoutElse(thenStmt)) {
            thenStmt = JsBlock(thenStmt)
        }

        withNestedIndent(thenStmt) {
            if (thenStmt is JsBlock && elseStatement != null) {
                state.needsLineBreakAfterBlock = false
            }

            withEmptySourceMapping {
                accept(materialize(thenStmt))
            }
        }

        if (elseStatement != null) {
            if (state.needsSemicolon) {
                semi()
                newline()
            } else {
                space()
                state.needsSemicolon = true
            }
            p.print(Chars.ELSE)

            if (elseStatement !is JsIf) {
                nestedPush(elseStatement)
            } else {
                space()
            }

            withEmptySourceMapping {
                accept(materialize(elseStatement))
            }

            if (elseStatement !is JsIf) {
                nestedPop(elseStatement)
            }
        }
    }

    override fun visitInvocation(invocation: JsInvocation) {
        withSourceMapping(invocation.source) {
            withComments(invocation) {
                printPair(invocation, invocation.qualifier)

                leftParen()
                printExpressions(invocation.arguments)
                rightParen()
            }
        }
    }

    override fun visitLabel(x: JsLabel) {
        nameOf(x)
        colon()
        space()

        withEmptySourceMapping {
            accept(x.statement)
        }
    }

    override fun visitNameRef(nameRef: JsNameRef) {
        withComments(nameRef) {
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

            withSourceMapping(nameRef.source) {
                p.print(nameRef.ident)
            }
        }
    }

    override fun visitNew(x: JsNew) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.NEW)
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
                state.needsSemicolon = true
            }
        }
    }

    override fun visitNull(x: JsNullLiteral) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.NULL)
            }
        }
    }

    override fun visitInt(x: JsIntLiteral) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(x.value)
            }
        }
    }

    override fun visitDouble(x: JsDoubleLiteral) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(x.value)
            }
        }
    }

    override fun visitBigInt(x: JsBigIntLiteral) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(x.value.toString())
                p.print('n')
            }
        }
    }

    override fun visitObjectLiteral(x: JsObjectLiteral) {
        withSourceMapping(x.source) {
            withComments(x) {
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

                    withSourceMapping(item.source) {
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
                            null -> {}
                        }
                    }
                }

                if (x.isMultiline) {
                    p.indentOut()
                    newline()
                }

                p.print('}')
            }
        }
    }

    override fun visitParameter(x: JsParameter) {
        withSourceMapping(x.source) {
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
        }
    }

    override fun visitPostfixOperation(x: JsPostfixOperation) {
        withSourceMapping(x.source) {
            withComments(x) {
                // unary operators always associate correctly (I think)
                printPair(x, x.arg)
                p.print(x.operator.symbol)
            }
        }
    }

    override fun visitPrefixOperation(x: JsPrefixOperation) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(x.operator.symbol)

                if (spaceCalc(x.operator, x.arg)) {
                    space()
                }

                // unary operators always associate correctly (I think)
                printPair(x, x.arg)
            }
        }
    }

    override fun visitProgram(x: JsProgram) {
        x.acceptChildren(this)
    }

    override fun visitRegExp(x: JsRegExp) {
        withSourceMapping(x.source) {
            withComments(x) {
                slash()
                p.print(x.pattern)
                slash()

                x.flags?.let {
                    p.print(it)
                }
            }
        }
    }

    override fun visitReturn(x: JsReturn) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.RETURN)
                x.expression?.let {
                    space()
                    accept(it)
                }
            }
        }
    }

    override fun visitString(x: JsStringLiteral) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(javaScriptString(x.value))
            }
        }
    }

    override fun visitTemplateString(x: JsTemplateStringLiteral) {
        withSourceMapping(x.source) {
            withComments(x) {
                accept(x.tag)

                p.print('`')
                for (segment in x.segments) {
                    accept(segment)
                }
                p.print('`')
            }
        }
    }

    override fun visitTemplateSegmentString(x: JsTemplateStringLiteral.Segment.StringLiteral) {
        withSourceMapping(x.source) {
            p.print(escapeTemplateStringSegment(x.value))
        }
    }

    override fun visitTemplateSegmentInterpolation(x: JsTemplateStringLiteral.Segment.Interpolation) {
        withSourceMapping(x.source) {
            p.print($$"${")
            accept(x.expression)
            p.print('}')
        }
    }

    override fun visit(x: JsSwitch) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.SWITCH)
                space()

                leftParen()
                accept(x.expression)
                rightParen()
            }
        }

        withEmptySourceMapping {
            space()

            blockOpen()
            acceptList(x.cases)
            blockClose()
        }
    }

    override fun visitThis(x: JsThisRef) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.THIS)
            }
        }
    }

    override fun visitSuper(x: JsSuperRef) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.SUPER)
            }
        }
    }

    override fun visitThrow(x: JsThrow) {
        withSourceMapping(x.source) {
            withComments(x) {
                p.print(Chars.THROW)
                space()

                accept(x.expression)
            }
        }
    }

    override fun visitTry(x: JsTry) {
        withComments(x) {
            withSourceMapping(x.source) {
                p.print(Chars.TRY)
                space()
                state.needsLineBreakAfterBlock = false
            }

            accept(x.tryBlock)
            acceptList(x.catches)

            val finallyBlock = x.finallyBlock
            if (finallyBlock != null) {
                p.print(Chars.FINALLY)
                space()

                accept(finallyBlock)
            }
        }
    }

    override fun visit(x: JsVars.JsVar) {
        withSourceMapping(x.source) {
            withComments(x) {
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
            }
        }
    }

    override fun visitVars(x: JsVars) {
        withSourceMapping(x.source) {
            withComments(x) {
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
            }
        }
    }

    override fun visitSingleLineComment(comment: JsSingleLineComment) {
        if (state.needsSemicolon && state.isInsideComments) {
            semi()
            space()
        }
        p.print("//")
        p.print(comment.text)
        newline()
        state.needsSemicolon = false
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

        state.needsSemicolon = true
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
        withSourceMapping(pattern.source) {
            withComments(pattern) {
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
            }
        }
    }

    override fun visitObjectPatternDeclarable(pattern: JsDeclarable.ObjectPattern) {
        withSourceMapping(pattern.source) {
            withComments(pattern) {
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
            }
        }
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

    //
    // Sourcemap info producing

    private inline fun withSourceMapping(location: JsLocationWithSource?, f: () -> Unit) {
        pushSourceInfo(location)
        f()
        popSourceInfo()
    }

    private inline fun withEmptySourceMapping(f: () -> Unit) {
        sourceLocationConsumer.pushSourceInfo(null)
        f()
        sourceLocationConsumer.popSourceInfo()
    }

    private fun pushSourceInfo(location: JsLocationWithSource?) {
        p.maybeIndent()
        sourceInfoStack.add(location)
        if (location != null) {
            sourceLocationConsumer.pushSourceInfo(location)
        }
    }

    private fun popSourceInfo() {
        if (sourceInfoStack.isNotEmpty() && sourceInfoStack.removeLast() != null) {
            sourceLocationConsumer.popSourceInfo()
        }
    }

    private inline fun withDeclaration(declaration: JsFunction, f: () -> Unit) {
        pushDeclaration(declaration)
        f()
        popDeclaration()
    }

    private fun pushDeclaration(declaration: JsFunction) {
        sourceLocationConsumer.pushDeclarationInfo(declaration.source)
    }

    private fun popDeclaration() {
        sourceLocationConsumer.popDeclarationInfo()
    }

    //
    // Preserving comments

    private inline fun withComments(node: JsNode, f: () -> Unit) {
        printCommentsBeforeNode(node)
        f()
        printCommentsAfterNode(node)
    }

    private fun printCommentsBeforeNode(x: JsNode) {
        printComments(x.commentsBeforeNode, false)
    }

    private fun printCommentsAfterNode(x: JsNode) {
        printComments(x.commentsAfterNode, true)
    }

    private fun printComments(comments: List<JsComment>?, isAfterNode: Boolean) {
        if (comments == null) return

        val previousNeedSemi = state.needsSemicolon
        state.needsSemicolon = isAfterNode
        state.isInsideComments = true

        for (comment in comments) {
            comment.accept(this)
        }

        state.isInsideComments = false

        if (!isAfterNode) {
            state.needsSemicolon = previousNeedSemi
        }
    }

    //
    // Printing helpers

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

    // constructor <declaration>
    private fun printConstructor(x: JsFunction) {
        withDeclaration(x) {
            withSourceMapping(x.source) {
                p.print(Chars.CONSTRUCTOR)
                printFunction(x)
            }
        }
    }

    // [static?] [get|set?] <declaration>
    private fun printClassMember(x: JsFunction) {
        withDeclaration(x) {
            withSourceMapping(x.source) {
                if (x.isStatic) {
                    p.print(Chars.STATIC)
                    space()
                }

                when {
                    x.isGetter -> {
                        p.print(Chars.GET)
                        space()
                    }
                    x.isSetter -> {
                        p.print(Chars.SET)
                        space()
                    }
                }

                printFunction(x)
            }
        }
    }

    // (<params>) => { <body> }
    private fun printEs6Arrow(x: JsFunction) {
        withSourceMapping(x.source) {
            printFunctionParameterList(x.parameters)
            space()
            arrow()
            space()
            val body = x.body
            when (val firstStatement = body.statements.getOrNull(0)) {
                is JsReturn if body.statements.size == 1 ->
                    firstStatement.expression.accept(this)
                else -> {
                    state.needsLineBreakAfterBlock = false
                    withEmptySourceMapping {
                        printJsBlock(body, true, x.body.source)
                    }
                }
            }
        }

        state.needsSemicolon = true
    }

    // function <declaration>
    private fun printRegularFunction(x: JsFunction) {
        withDeclaration(x) {
            withSourceMapping(x.source) {
                p.print(Chars.FUNCTION)
                space()
                printFunction(x)
            }
        }
    }

    // [name|computedName](<params>) { <body> }
    private fun printFunction(x: JsFunction) {
        if (x.isGenerator)
            p.print(Chars.GENERATOR)

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

        state.needsLineBreakAfterBlock = false

        withEmptySourceMapping {
            printJsBlock(x.body, true, x.body.source)
        }

        state.needsSemicolon = true
    }

    private fun printFunctionParameterList(parameters: List<JsParameter>) {
        leftParen()

        withEmptySourceMapping {
            var notFirst = false
            for (param in parameters) {
                notFirst = sepCommaSpace(notFirst)
                printCommentsBeforeNode(param)
                accept(param)
                printCommentsAfterNode(param)
            }
        }

        rightParen()
    }

    private fun printJsBlock(x: JsBlock, finalNewline: Boolean, defaultClosingBraceLocation: JsLocationWithSource?) {
        var finalNewline = finalNewline
        if (!state.needsLineBreakAfterBlock) {
            finalNewline = false
            state.needsLineBreakAfterBlock = true
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
            val isGlobal = x.isTransparent || state.globalBlocks.contains(x)

            val statement = iterator.next()
            if (statement is JsEmpty) {
                continue
            }

            state.needsSemicolon = true
            var stmtIsGlobalBlock = false
            if (isGlobal) {
                if (statement is JsBlock) {
                    // A block inside a global block is still considered global
                    stmtIsGlobalBlock = true
                    state.globalBlocks.add(statement)
                }
            }

            accept<JsStatement?>(statement)
            if (stmtIsGlobalBlock) {
                state.globalBlocks.remove(statement)
            }
            if (state.needsSemicolon) {
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
        state.needsSemicolon = false
        printCommentsAfterNode(x)
    }

    private fun materialize(statement: JsStatement?): JsStatement? {
        return when (statement) {
            is JsCompositeBlock -> JsBlock(statement)
            else -> statement
        }
    }

    private fun continueOrBreakLabel(x: JsContinue) {
        x.label?.let {
            space()
            p.print(it.ident)
        }
    }

    private fun printSwitchMemberStatements(x: JsSwitchMember) {
        p.indentIn()
        for (stmt in x.statements) {
            state.needsSemicolon = true
            accept(stmt)
            if (state.needsSemicolon) {
                semi()
            }
            newline()
        }
        p.indentOut()
        state.needsSemicolon = false
    }

    private fun printIterableLoop(x: JsIterableLoop, separatorChars: CharArray) {
        withSourceMapping(x.source) {
            withComments(x) {
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
            }
        }

        val body = materialize(x.body)
        withNestedIndent(body) {
            withEmptySourceMapping {
                accept(body)
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

    private inline fun withNestedIndent(statement: JsStatement?, f: () -> Unit) {
        nestedPush(statement)
        f()
        nestedPop(statement)
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

    private fun nestedPop(statement: JsStatement?) {
        if (statement !is JsBlock) {
            p.indentOut()
        }
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

    private fun sepCommaSpace(isNonFirst: Boolean): Boolean {
        if (isNonFirst) {
            p.print(',')
            space()
        }
        return true
    }

    private fun newline() {
        p.newline()
        sourceLocationConsumer.newLine()
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
        p.print(Chars.FOR)
    }

    private fun _if() {
        p.print(Chars.IF)
    }

    private fun nameDef(name: JsName) {
        p.print(name.ident)
    }

    private fun nameOf(hasName: HasName) {
        nameDef(hasName.name)
    }

    private fun leftParen() {
        p.print('(')
    }

    private fun leftSquare() {
        p.print('[')
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

    private fun slash() {
        p.print('/')
    }

    private fun space() {
        p.print(' ')
    }

    private fun varModifier(variant: JsVars.Variant) {
        when (variant) {
            JsVars.Variant.Var -> _var()
            JsVars.Variant.Let -> let()
            JsVars.Variant.Const -> _const()
        }
    }

    private fun _var() {
        p.print(Chars.VAR)
    }

    private fun let() {
        p.print(Chars.LET)
    }

    private fun _const() {
        p.print(Chars.CONST)
    }

    private fun _while() {
        p.print(Chars.WHILE)
    }

    private fun ellipsis() {
        p.print(Chars.ELLIPSIS)
    }

    companion object {
        //
        // String escaping

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
                appendEscapedString(str, quoteChar, Chars.STRING_ESCAPE_MAPPING)
                append(quoteChar)
                appendEscapeClosingTags()
            }
        }

        private fun escapeTemplateStringSegment(str: String): String {
            return buildString {
                appendEscapedString(str, '`', Chars.TEMPLATE_ESCAPE_MAPPING)
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
                    append(Chars.HEX_DIGITS[digit])
                    shift -= 4
                }
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

    private object Chars {
        val BREAK = "break".toCharArray()
        val CASE = "case".toCharArray()
        val CATCH = "catch".toCharArray()
        val CLASS = "class".toCharArray()
        val CONSTRUCTOR = "constructor".toCharArray()
        val CONTINUE = "continue".toCharArray()
        val YIELD = "yield".toCharArray()
        val YIELD_STAR = "yield*".toCharArray()
        val DEBUGGER = "debugger".toCharArray()
        val DEFAULT = "default".toCharArray()
        val DO = "do".toCharArray()
        val ELSE = "else".toCharArray()
        val EXTENDS = "extends".toCharArray()
        val FALSE = "false".toCharArray()
        val FINALLY = "finally".toCharArray()
        val FOR = "for".toCharArray()
        val FUNCTION = "function".toCharArray()
        val STATIC = "static".toCharArray()
        val GET = "get".toCharArray()
        val SET = "set".toCharArray()
        val IF = "if".toCharArray()
        val IN = "in".toCharArray()
        val OF = "of".toCharArray()
        val NEW = "new".toCharArray()
        val NULL = "null".toCharArray()
        val RETURN = "return".toCharArray()
        val SWITCH = "switch".toCharArray()
        val THIS = "this".toCharArray()
        const val GENERATOR = '*'

        val SUPER = "super".toCharArray()
        val THROW = "throw".toCharArray()
        val TRUE = "true".toCharArray()
        val TRY = "try".toCharArray()
        val VAR = "var".toCharArray()
        val LET = "let".toCharArray()
        val CONST = "const".toCharArray()
        val WHILE = "while".toCharArray()
        val ELLIPSIS = "...".toCharArray()
        val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

        val STRING_ESCAPE_MAPPING = mapOf(
            '\b' to 'b'.code,
            '\u000c' to 'f'.code,
            '\n' to 'n'.code,
            '\r' to 'r'.code,
            '\t' to 't'.code,
            '\\' to '\\'.code
        )

        val TEMPLATE_ESCAPE_MAPPING = STRING_ESCAPE_MAPPING + mapOf(
            '$' to '$'.code
        )
    }

    //
    // State

    /**
     * @param globalBlocks "Global" blocks are either the global block of a fragment, or a block
     * nested directly within some other global block. This definition matters
     * because the statements designated by statementEnds and statementStarts are
     * those that appear directly within these global blocks.
     */
    private class GenerationState(
        var isInsideComments: Boolean = false,
        var needsSemicolon: Boolean = true,
        var needsLineBreakAfterBlock: Boolean = true,
        val globalBlocks: MutableSet<JsBlock> = ObjectOpenHashSet(),
    )
}
