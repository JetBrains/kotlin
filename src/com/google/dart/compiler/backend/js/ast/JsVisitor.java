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

    public <T extends JsNode> T accept(T node) {
        doTraverse(node, UNMODIFIABLE_CONTEXT);
        return node;
    }

    public final <T extends JsNode> void acceptList(List<T> collection) {
        for (T node : collection) {
            doTraverse(node, UNMODIFIABLE_CONTEXT);
        }
    }

    public JsExpression acceptLvalue(JsExpression expr) {
        doTraverse(expr, LVALUE_CONTEXT);
        return expr;
    }

    public final <T extends JsNode> void acceptWithInsertRemove(List<T> collection) {
        for (T node : collection) {
            doTraverse(node, UNMODIFIABLE_CONTEXT);
        }
    }

    public boolean visit(JsArrayAccess x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsArrayLiteral x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsBinaryOperation x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsBlock x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsLiteral.JsBooleanLiteral x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsBreak x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsCase x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsCatch x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsConditional x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsContinue x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsDebugger x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsDefault x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsDoWhile x, JsContext ctx) {
        return true;
    }

    public void visit(JsEmpty x, JsContext ctx) {
    }

    public boolean visit(JsExpressionStatement x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsFor x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsForIn x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsFunction x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsIf x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsInvocation x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsLabel x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsNameRef x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsNew x, JsContext ctx) {
        return true;
    }

    public void visit(JsNullLiteral x, JsContext ctx) {
    }

    public void visit(JsNumberLiteral.JsIntLiteral x, JsContext ctx) {
    }

    public void visit(JsNumberLiteral.JsDoubleLiteral x, JsContext ctx) {
    }

    public boolean visit(JsObjectLiteral x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsParameter x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsPostfixOperation x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsPrefixOperation x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsProgram x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsProgramFragment x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsPropertyInitializer x, JsContext ctx) {
        return true;
    }

    public void visit(JsRegExp x, JsContext ctx) {
    }

    public boolean visit(JsReturn x, JsContext ctx) {
        return true;
    }

    public void visit(JsStringLiteral x, JsContext ctx) {
    }

    public boolean visit(JsSwitch x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsLiteral.JsThisRef x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsThrow x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsTry x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsVar x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsVars x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsWhile x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsDocComment x, JsContext context) {
        return true;
    }

    protected void doTraverse(JsNode node, JsContext context) {
        node.accept(this, context);
    }
}