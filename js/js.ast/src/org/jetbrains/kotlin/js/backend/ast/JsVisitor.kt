// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast

import org.jetbrains.kotlin.js.backend.ast.JsVars.JsVar

abstract class JsVisitor {
    open fun <T : JsNode?> accept(node: T) {
        node?.accept(this)
    }

    fun <T : JsNode> acceptList(collection: List<T>) {
        for (node in collection) {
            accept(node)
        }
    }

    fun acceptLvalue(expression: JsExpression) {
        accept(expression)
    }

    fun <T : JsNode> acceptWithInsertRemove(collection: List<T>) {
        for (node in collection) {
            accept(node)
        }
    }

    open fun visitArrayAccess(x: JsArrayAccess): Unit =
            visitElement(x)

    open fun visitArray(x: JsArrayLiteral): Unit =
            visitElement(x)

    open fun visitBinaryExpression(x: JsBinaryOperation): Unit =
            visitElement(x)

    open fun visitBlock(x: JsBlock): Unit =
            visitElement(x)

    open fun visitBoolean(x: JsBooleanLiteral): Unit =
            visitElement(x)

    open fun visitBreak(x: JsBreak): Unit =
            visitElement(x)

    open fun visitCase(x: JsCase): Unit =
            visitElement(x)

    open fun visitCatch(x: JsCatch): Unit =
            visitElement(x)

    open fun visitClass(x: JsClass): Unit =
            visitElement(x)

    open fun visitConditional(x: JsConditional): Unit =
            visitElement(x)

    open fun visitContinue(x: JsContinue): Unit =
            visitElement(x)

    open fun visitYield(x: JsYield): Unit =
        visitElement(x)

    open fun visitDebugger(x: JsDebugger): Unit =
            visitElement(x)

    open fun visitDefault(x: JsDefault): Unit =
            visitElement(x)

    open fun visitDoWhile(x: JsDoWhile): Unit =
            visitLoop(x)

    open fun visitEmpty(x: JsEmpty): Unit =
            visitElement(x)

    open fun visitExpressionStatement(x: JsExpressionStatement): Unit =
            visitElement(x)

    open fun visitFor(x: JsFor): Unit =
            visitLoop(x)

    open fun visitForIn(x: JsForIn): Unit =
            visitLoop(x)

    open fun visitFunction(x: JsFunction): Unit =
            visitElement(x)

    open fun visitIf(x: JsIf): Unit =
            visitElement(x)

    open fun visitInvocation(invocation: JsInvocation): Unit =
            visitElement(invocation)

    open fun visitLabel(x: JsLabel): Unit =
            visitElement(x)

    open fun visitLoop(x: JsLoop): Unit =
        visitElement(x)

    open fun visitNameRef(nameRef: JsNameRef): Unit =
            visitElement(nameRef)

    open fun visitNew(x: JsNew): Unit =
            visitElement(x)

    open fun visitNull(x: JsNullLiteral): Unit =
            visitElement(x)

    open fun visitInt(x: JsIntLiteral): Unit =
            visitElement(x)

    open fun visitDouble(x: JsDoubleLiteral): Unit =
            visitElement(x)

    open fun visitBigInt(x: JsBigIntLiteral): Unit =
            visitElement(x)

    open fun visitObjectLiteral(x: JsObjectLiteral): Unit =
            visitElement(x)

    open fun visitParameter(x: JsParameter): Unit =
            visitElement(x)

    open fun visitPostfixOperation(x: JsPostfixOperation): Unit =
            visitElement(x)

    open fun visitPrefixOperation(x: JsPrefixOperation): Unit =
            visitElement(x)

    open fun visitProgram(x: JsProgram): Unit =
            visitElement(x)

    open fun visitPropertyInitializer(x: JsPropertyInitializer): Unit =
            visitElement(x)

    open fun visitRegExp(x: JsRegExp): Unit =
            visitElement(x)

    open fun visitReturn(x: JsReturn): Unit =
            visitElement(x)

    open fun visitString(x: JsStringLiteral): Unit =
            visitElement(x)

    open fun visit(x: JsSwitch): Unit =
            visitElement(x)

    open fun visitThis(x: JsThisRef): Unit =
            visitElement(x)

    open fun visitSuper(x: JsSuperRef): Unit =
            visitElement(x)

    open fun visitThrow(x: JsThrow): Unit =
            visitElement(x)

    open fun visitTry(x: JsTry): Unit =
            visitElement(x)

    open fun visit(x: JsVar): Unit =
            visitElement(x)

    open fun visitVars(x: JsVars): Unit =
            visitElement(x)

    open fun visitWhile(x: JsWhile): Unit =
            visitLoop(x)

    open fun visitDocComment(comment: JsDocComment): Unit =
            visitElement(comment)

    open fun visitSingleLineComment(comment: JsSingleLineComment): Unit =
            visitElement(comment)

    open fun visitMultiLineComment(comment: JsMultiLineComment): Unit =
            visitElement(comment)

    open fun visitExport(export: JsExport): Unit =
            visitElement(export)

    open fun visitImport(import: JsImport): Unit =
        visitElement(import)

    protected open fun visitElement(node: JsNode) {
    }
}
