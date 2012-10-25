// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.*;

/**
 * Searches for method invocations in constructor expressions that would not
 * normally be surrounded by parentheses.
 */
public class JsConstructExpressionVisitor extends RecursiveJsVisitor {
    public static boolean exec(JsExpression expression) {
        if (JsPrecedenceVisitor.exec(expression) < JsPrecedenceVisitor.PRECEDENCE_NEW) {
            return true;
        }
        JsConstructExpressionVisitor visitor = new JsConstructExpressionVisitor();
        visitor.accept(expression);
        return visitor.containsInvocation;
    }

    private boolean containsInvocation;

    private JsConstructExpressionVisitor() {
    }

    /**
     * We only look at the array expression since the index has its own scope.
     */
    @Override
    public void visitArrayAccess(JsArrayAccess x, JsContext ctx) {
        accept(x.getArrayExpression());
    }

    /**
     * Array literals have their own scoping.
     */
    @Override
    public void visitArray(JsArrayLiteral x, JsContext ctx) {
    }

    /**
     * Functions have their own scoping.
     */
    @Override
    public void visitFunction(JsFunction x, JsContext ctx) {
    }

    @Override
    public void visitInvocation(JsInvocation x, JsContext ctx) {
        containsInvocation = true;
    }

    @Override
    public void visitNameRef(JsNameRef x, JsContext ctx) {
        if (!x.isLeaf()) {
            accept(x.getQualifier());
        }
    }

    /**
     * New constructs bind to the nearest set of parentheses.
     */
    @Override
    public void visitNew(JsNew x, JsContext ctx) {
    }

    /**
     * Object literals have their own scope.
     */
    @Override
    public void visitObjectLiteral(JsObjectLiteral x, JsContext ctx) {
    }

    /**
     * We only look at nodes that would not normally be surrounded by parentheses.
     */
    @Override
    public <T extends JsNode> T accept(T node) {
        // Assign to Object to prevent 'inconvertible types' compile errors due
        // to http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6548436
        // reproducible in jdk1.6.0_02.
        if (node instanceof JsExpression) {
            JsExpression expression = (JsExpression) node;
            int precedence = JsPrecedenceVisitor.exec(expression);
            // Only visit expressions that won't automatically be surrounded by
            // parentheses
            if (precedence < JsPrecedenceVisitor.PRECEDENCE_NEW) {
                return node;
            }
        }
        return super.accept(node);
    }
}
