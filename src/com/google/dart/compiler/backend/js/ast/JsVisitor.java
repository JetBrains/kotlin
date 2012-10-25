// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.backend.js.ast.JsVars.JsVar;

import java.util.List;

public abstract class JsVisitor {
    protected static final JsContext LVALUE_CONTEXT = new JsContext() {
        @Override
        public boolean canInsert() {
            return false;
        }

        @Override
        public boolean canRemove() {
            return false;
        }

        @Override
        public void insertAfter(JsNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void insertBefore(JsNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLvalue() {
            return true;
        }

        @Override
        public void removeMe() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceMe(JsNode node) {
            throw new UnsupportedOperationException();
        }
    };

    protected static final JsContext UNMODIFIABLE_CONTEXT = new JsContext() {
        @Override
        public boolean canInsert() {
            return false;
        }

        @Override
        public boolean canRemove() {
            return false;
        }

        @Override
        public void insertAfter(JsNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void insertBefore(JsNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLvalue() {
            return false;
        }

        @Override
        public void removeMe() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceMe(JsNode node) {
            throw new UnsupportedOperationException();
        }
    };

    public <T extends JsNode> void accept(T node) {
        doAccept(node, UNMODIFIABLE_CONTEXT);
    }

    public final <T extends JsNode> void acceptList(List<T> collection) {
        for (T node : collection) {
            doAccept(node, UNMODIFIABLE_CONTEXT);
        }
    }

    public void acceptLvalue(JsExpression expression) {
        doAccept(expression, LVALUE_CONTEXT);
    }

    public final <T extends JsNode> void acceptWithInsertRemove(List<T> collection) {
        for (T node : collection) {
            doAccept(node, UNMODIFIABLE_CONTEXT);
        }
    }

    public void visitArrayAccess(JsArrayAccess x, JsContext context) {
        visitElement(x, context);
    }

    public void visitArray(JsArrayLiteral x, JsContext context) {
        visitElement(x, context);
    }

    public void visitBinaryExpression(JsBinaryOperation x, JsContext context) {
        visitElement(x, context);
    }

    public void visitBlock(JsBlock x, JsContext context) {
        visitElement(x, context);
    }

    public void visitBoolean(JsLiteral.JsBooleanLiteral x, JsContext context) {
        visitElement(x, context);
    }

    public void visitBreak(JsBreak x, JsContext context) {
        visitElement(x, context);
    }

    public void visitCase(JsCase x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitCatch(JsCatch x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitConditional(JsConditional x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitContinue(JsContinue x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitDebugger(JsDebugger x, JsContext context) {
        visitElement(x, context);
    }

    public void visitDefault(JsDefault x, JsContext context) {
        visitElement(x, context);
    }

    public void visitDoWhile(JsDoWhile x, JsContext context) {
        visitElement(x, context);
    }

    public void visitEmpty(JsEmpty x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitExpressionStatement(JsExpressionStatement x, JsContext context) {
        visitElement(x, context);
    }

    public void visitFor(JsFor x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitForIn(JsForIn x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitFunction(JsFunction x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitIf(JsIf x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitInvocation(JsInvocation x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitLabel(JsLabel x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitNameRef(JsNameRef x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitNew(JsNew x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitNull(JsNullLiteral x, JsContext ctx) {
        visitElement(x, ctx);
    }

    public void visitInt(JsNumberLiteral.JsIntLiteral x, JsContext context) {
        visitElement(x, context);
    }

    public void visitDouble(JsNumberLiteral.JsDoubleLiteral x, JsContext context) {
        visitElement(x, context);
    }

    public void visitObjectLiteral(JsObjectLiteral x, JsContext context) {
        visitElement(x, context);
    }

    public void visitParameter(JsParameter x, JsContext context) {
        visitElement(x, context);
    }

    public void visitPostfixOperation(JsPostfixOperation x, JsContext context) {
        visitElement(x, context);
    }

    public void visitPrefixOperation(JsPrefixOperation x, JsContext context) {
        visitElement(x, context);
    }

    public void visitProgram(JsProgram x, JsContext context) {
        visitElement(x, context);
    }

    public void visitProgramFragment(JsProgramFragment x, JsContext context) {
        visitElement(x, context);
    }

    public void visitPropertyInitializer(JsPropertyInitializer x, JsContext context) {
        visitElement(x, context);
    }

    public void visitRegExp(JsRegExp x, JsContext context) {
        visitElement(x, context);
    }

    public void visitReturn(JsReturn x, JsContext context) {
        visitElement(x, context);
    }

    public void visitString(JsStringLiteral x, JsContext context) {
        visitElement(x, context);
    }

    public void visit(JsSwitch x, JsContext context) {
        visitElement(x, context);
    }

    public void visitThis(JsLiteral.JsThisRef x, JsContext context) {
        visitElement(x, context);
    }

    public void visitThrow(JsThrow x, JsContext context) {
        visitElement(x, context);
    }

    public void visitTry(JsTry x, JsContext context) {
        visitElement(x, context);
    }

    public void visit(JsVar x, JsContext context) {
        visitElement(x, context);
    }

    public void visitVars(JsVars x, JsContext context) {
        visitElement(x, context);
    }

    public void visitWhile(JsWhile x, JsContext context) {
        visitElement(x, context);
    }

    public void visitDocComment(JsDocComment comment, JsContext context) {
        visitElement(comment, context);
    }

    protected void visitElement(JsNode node, JsContext context) {
    }

    protected void doAccept(JsNode node, JsContext context) {
        node.accept(this, context);
    }
}