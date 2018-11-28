// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

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
public class JsFor extends SourceInfoAwareJsNode implements JsLoop {
    private JsStatement body;
    private JsExpression condition;
    private JsExpression incrementExpression;
    private JsExpression initExpression;
    private JsVars initVars;

    public JsFor(JsVars initVars, JsExpression condition, JsExpression incrementExpression) {
        this(initVars, condition, incrementExpression, null);
    }

    public JsFor(JsVars initVars, JsExpression condition, JsExpression incrementExpression, JsStatement body) {
        this.initVars = initVars;
        this.incrementExpression = incrementExpression;
        this.condition = condition;
        this.body = body;
        initExpression = null;
    }

    public JsFor(JsExpression initExpression, JsExpression condition, JsExpression incrementExpression) {
        this(initExpression, condition, incrementExpression, null);
    }

    public JsFor(JsExpression initExpression, JsExpression condition, JsExpression incrementExpression, JsStatement body) {
        this.initExpression = initExpression;
        this.incrementExpression = incrementExpression;
        this.condition = condition;
        this.body = body;
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
    public void accept(JsVisitor v) {
        v.visitFor(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        assert (!(initExpression != null && initVars != null));

        if (initExpression != null) {
            visitor.accept(initExpression);
        }
        else if (initVars != null) {
            visitor.accept(initVars);
        }

        if (condition != null) {
            visitor.accept(condition);
        }

        if (incrementExpression != null) {
            visitor.accept(incrementExpression);
        }
        visitor.accept(body);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            assert (!(initExpression != null && initVars != null));

            if (initExpression != null) {
                initExpression = v.accept(initExpression);
            } else if (initVars != null) {
                JsStatement newInitVars = v.<JsStatement>acceptStatement(initVars);
                if (newInitVars instanceof JsVars) {
                    initVars = (JsVars) newInitVars;
                }
                else {
                    initVars = null;
                    if (newInitVars instanceof JsExpressionStatement) {
                        initExpression = ((JsExpressionStatement) newInitVars).getExpression();
                    } else if (newInitVars != null) {
                        ctx.addPrevious(newInitVars);
                    }
                }
            }

            if (condition != null) {
                condition = v.accept(condition);
            }

            if (incrementExpression != null) {
                incrementExpression = v.accept(incrementExpression);
            }
            body = v.acceptStatement(body);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsFor deepCopy() {
        JsStatement bodyCopy = AstUtil.deepCopy(body);
        JsExpression conditionCopy = AstUtil.deepCopy(condition);
        JsExpression incrementalExprCopy = AstUtil.deepCopy(incrementExpression);

        JsFor result;
        if (initVars != null) {
            result = new JsFor(initVars.deepCopy(), conditionCopy, incrementalExprCopy, bodyCopy);
        } else {
            result = new JsFor(initExpression != null ? initExpression.deepCopy() : null, conditionCopy, incrementalExprCopy, bodyCopy);
        }

        return result.withMetadataFrom(this);
    }
}
