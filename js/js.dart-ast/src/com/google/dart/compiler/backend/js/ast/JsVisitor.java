// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.backend.js.ast.JsVars.JsVar;

import java.util.List;

public abstract class JsVisitor {
    public <T extends JsNode> void accept(T node) {
        node.accept(this);
    }

    public final <T extends JsNode> void acceptList(List<T> collection) {
        for (T node : collection) {
            accept(node);
        }
    }

    public void acceptLvalue(JsExpression expression) {
        accept(expression);
    }

    public final <T extends JsNode> void acceptWithInsertRemove(List<T> collection) {
        for (T node : collection) {
            accept(node);
        }
    }

    public void visitArrayAccess(JsArrayAccess x) {
        visitElement(x);
    }

    public void visitArray(JsArrayLiteral x) {
        visitElement(x);
    }

    public void visitBinaryExpression(JsBinaryOperation x) {
        visitElement(x);
    }

    public void visitBlock(JsBlock x) {
        visitElement(x);
    }

    public void visitBoolean(JsLiteral.JsBooleanLiteral x) {
        visitElement(x);
    }

    public void visitBreak(JsBreak x) {
        visitElement(x);
    }

    public void visitCase(JsCase x) {
        visitElement(x);
    }

    public void visitCatch(JsCatch x) {
        visitElement(x);
    }

    public void visitConditional(JsConditional x) {
        visitElement(x);
    }

    public void visitContinue(JsContinue x) {
        visitElement(x);
    }

    public void visitDebugger(JsDebugger x) {
        visitElement(x);
    }

    public void visitDefault(JsDefault x) {
        visitElement(x);
    }

    public void visitDoWhile(JsDoWhile x) {
        visitElement(x);
    }

    public void visitEmpty(JsEmpty x) {
        visitElement(x);
    }

    public void visitExpressionStatement(JsExpressionStatement x) {
        visitElement(x);
    }

    public void visitFor(JsFor x) {
        visitElement(x);
    }

    public void visitForIn(JsForIn x) {
        visitElement(x);
    }

    public void visitFunction(JsFunction x) {
        visitElement(x);
    }

    public void visitIf(JsIf x) {
        visitElement(x);
    }

    public void visitInvocation(JsInvocation invocation) {
        visitElement(invocation);
    }

    public void visitLabel(JsLabel x) {
        visitElement(x);
    }

    public void visitNameRef(JsNameRef nameRef) {
        visitElement(nameRef);
    }

    public void visitNew(JsNew x) {
        visitElement(x);
    }

    public void visitNull(JsNullLiteral x) {
        visitElement(x);
    }

    public void visitInt(JsNumberLiteral.JsIntLiteral x) {
        visitElement(x);
    }

    public void visitDouble(JsNumberLiteral.JsDoubleLiteral x) {
        visitElement(x);
    }

    public void visitObjectLiteral(JsObjectLiteral x) {
        visitElement(x);
    }

    public void visitParameter(JsParameter x) {
        visitElement(x);
    }

    public void visitPostfixOperation(JsPostfixOperation x) {
        visitElement(x);
    }

    public void visitPrefixOperation(JsPrefixOperation x) {
        visitElement(x);
    }

    public void visitProgram(JsProgram x) {
        visitElement(x);
    }

    public void visitProgramFragment(JsProgramFragment x) {
        visitElement(x);
    }

    public void visitPropertyInitializer(JsPropertyInitializer x) {
        visitElement(x);
    }

    public void visitRegExp(JsRegExp x) {
        visitElement(x);
    }

    public void visitReturn(JsReturn x) {
        visitElement(x);
    }

    public void visitString(JsStringLiteral x) {
        visitElement(x);
    }

    public void visit(JsSwitch x) {
        visitElement(x);
    }

    public void visitThis(JsLiteral.JsThisRef x) {
        visitElement(x);
    }

    public void visitThrow(JsThrow x) {
        visitElement(x);
    }

    public void visitTry(JsTry x) {
        visitElement(x);
    }

    public void visit(JsVar x) {
        visitElement(x);
    }

    public void visitVars(JsVars x) {
        visitElement(x);
    }

    public void visitWhile(JsWhile x) {
        visitElement(x);
    }

    public void visitDocComment(JsDocComment comment) {
        visitElement(comment);
    }

    protected void visitElement(JsNode node) {
    }
}