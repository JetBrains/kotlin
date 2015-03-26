// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast

import com.google.dart.compiler.backend.js.ast.JsVars.JsVar

public abstract class JsVisitor {
    public open fun <T : JsNode?> accept(node: T) {
        node?.accept(this)
    }

    public fun <T : JsNode> acceptList(collection: List<T>) {
        for (node in collection) {
            accept(node)
        }
    }

    public fun acceptLvalue(expression: JsExpression) {
        accept(expression)
    }

    public fun <T : JsNode> acceptWithInsertRemove(collection: List<T>) {
        for (node in collection) {
            accept(node)
        }
    }

    public open fun visitArrayAccess(x: JsArrayAccess): Unit =
            visitElement(x)

    public open fun visitArray(x: JsArrayLiteral): Unit =
            visitElement(x)

    public open fun visitBinaryExpression(x: JsBinaryOperation): Unit =
            visitElement(x)

    public open fun visitBlock(x: JsBlock): Unit =
            visitElement(x)

    public open fun visitBoolean(x: JsLiteral.JsBooleanLiteral): Unit =
            visitElement(x)

    public open fun visitBreak(x: JsBreak): Unit =
            visitElement(x)

    public open fun visitCase(x: JsCase): Unit =
            visitElement(x)

    public open fun visitCatch(x: JsCatch): Unit =
            visitElement(x)

    public open fun visitConditional(x: JsConditional): Unit =
            visitElement(x)

    public open fun visitContinue(x: JsContinue): Unit =
            visitElement(x)

    public open fun visitDebugger(x: JsDebugger): Unit =
            visitElement(x)

    public open fun visitDefault(x: JsDefault): Unit =
            visitElement(x)

    public open fun visitDoWhile(x: JsDoWhile): Unit =
            visitElement(x)

    public open fun visitEmpty(x: JsEmpty): Unit =
            visitElement(x)

    public open fun visitExpressionStatement(x: JsExpressionStatement): Unit =
            visitElement(x)

    public open fun visitFor(x: JsFor): Unit =
            visitElement(x)

    public open fun visitForIn(x: JsForIn): Unit =
            visitElement(x)

    public open fun visitFunction(x: JsFunction): Unit =
            visitElement(x)

    public open fun visitIf(x: JsIf): Unit =
            visitElement(x)

    public open fun visitInvocation(invocation: JsInvocation): Unit =
            visitElement(invocation)

    public open fun visitLabel(x: JsLabel): Unit =
            visitElement(x)

    public open fun visitNameRef(nameRef: JsNameRef): Unit =
            visitElement(nameRef)

    public open fun visitNew(x: JsNew): Unit =
            visitElement(x)

    public open fun visitNull(x: JsNullLiteral): Unit =
            visitElement(x)

    public open fun visitInt(x: JsNumberLiteral.JsIntLiteral): Unit =
            visitElement(x)

    public open fun visitDouble(x: JsNumberLiteral.JsDoubleLiteral): Unit =
            visitElement(x)

    public open fun visitObjectLiteral(x: JsObjectLiteral): Unit =
            visitElement(x)

    public open fun visitParameter(x: JsParameter): Unit =
            visitElement(x)

    public open fun visitPostfixOperation(x: JsPostfixOperation): Unit =
            visitElement(x)

    public open fun visitPrefixOperation(x: JsPrefixOperation): Unit =
            visitElement(x)

    public open fun visitProgram(x: JsProgram): Unit =
            visitElement(x)

    public open fun visitProgramFragment(x: JsProgramFragment): Unit =
            visitElement(x)

    public open fun visitPropertyInitializer(x: JsPropertyInitializer): Unit =
            visitElement(x)

    public open fun visitRegExp(x: JsRegExp): Unit =
            visitElement(x)

    public open fun visitReturn(x: JsReturn): Unit =
            visitElement(x)

    public open fun visitString(x: JsStringLiteral): Unit =
            visitElement(x)

    public open fun visit(x: JsSwitch): Unit =
            visitElement(x)

    public open fun visitThis(x: JsLiteral.JsThisRef): Unit =
            visitElement(x)

    public open fun visitThrow(x: JsThrow): Unit =
            visitElement(x)

    public open fun visitTry(x: JsTry): Unit =
            visitElement(x)

    public open fun visit(x: JsVar): Unit =
            visitElement(x)

    public open fun visitVars(x: JsVars): Unit =
            visitElement(x)

    public open fun visitWhile(x: JsWhile): Unit =
            visitElement(x)

    public open fun visitDocComment(comment: JsDocComment): Unit =
            visitElement(comment)

    protected open fun visitElement(node: JsNode) {
    }
}