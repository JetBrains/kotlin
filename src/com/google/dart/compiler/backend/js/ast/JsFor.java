// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A <code>for</code> statement. If specified at all, the initializer part is
 * either a declaration of one or more variables, in which case
 * {@link #getInitVars()} is used, or an expression, in which case
 * {@link #getInitExpression()} is used. In the latter case, the comma operator is
 * often used to create a compound expression.
 * <p/>
 * <p/>
 * Note that any of the parts of the <code>for</code> loop header can be
 * <code>null</code>, although the body will never be null.
 */
public class JsFor extends JsNodeImpl implements JsStatement {
    private JsStatement body;
    private JsExpression condition;
    private JsExpression incrementExpression;
    private JsExpression initExpression;
    private JsVars initVars;

    public JsFor(JsVars initVars, JsExpression condition, JsExpression incrementExpression) {
        this.initVars = initVars;
        this.incrementExpression = incrementExpression;
        this.condition = condition;
        initExpression = null;
    }

    public JsFor(JsExpression initExpression, JsExpression condition, JsExpression incrementExpression) {
        this.initExpression = initExpression;
        this.incrementExpression = incrementExpression;
        this.condition = condition;
        initVars = null;
    }

    public JsStatement getBody() {
        return body;
    }

    public JsExpression getCondition() {
        return condition;
    }

    public JsExpression getIncrementExpression() {
        return incrementExpression;
    }

    public JsExpression getInitExpression() {
        return initExpression;
    }

    public JsVars getInitVars() {
        return initVars;
    }

    public void setBody(JsStatement body) {
        this.body = body;
    }

    @Override
    public void traverse(JsVisitor v, JsContext context) {
        if (v.visit(this, context)) {
            assert (!(initExpression != null && initVars != null));

            if (initExpression != null) {
                initExpression = v.accept(initExpression);
            }
            else if (initVars != null) {
                initVars = v.accept(initVars);
            }

            if (condition != null) {
                condition = v.accept(condition);
            }

            if (incrementExpression != null) {
                incrementExpression = v.accept(incrementExpression);
            }
            body = v.accept(body);
        }
        v.endVisit(this, context);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.FOR;
    }
}
