// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a JavaScript catch clause.
 */
public class JsCatch extends SourceInfoAwareJsNode implements HasCondition {

    protected final JsCatchScope scope;
    private JsBlock body;
    private JsExpression condition;
    private JsParameter param;

    public JsCatch(JsScope parent, @NotNull String ident) {
        super();
        assert (parent != null);
        scope = new JsCatchScope(parent, ident);
        param = new JsParameter(scope.findName(ident));
    }

    public JsCatch(JsScope parent, @NotNull String ident, @NotNull JsStatement catchBody) {
        this(parent, ident);
        if (catchBody instanceof JsBlock) {
            body = (JsBlock) catchBody;
        } else {
            body = new JsBlock(catchBody);
        }
    }

    public JsBlock getBody() {
        return body;
    }

    @Override
    public JsExpression getCondition() {
        return condition;
    }

    public JsParameter getParameter() {
        return param;
    }

    public JsScope getScope() {
        return scope;
    }

    public void setBody(JsBlock body) {
        this.body = body;
    }

    @Override
    public void setCondition(JsExpression condition) {
        this.condition = condition;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitCatch(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(param);
        if (condition != null) {
            visitor.accept(condition);
        }
        visitor.accept(body);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            param = v.accept(param);
            if (condition != null) {
                condition = v.accept(condition);
            }
            body = v.acceptStatement(body);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsCatch deepCopy() {
        JsCatchScope scopeCopy = scope.copy();
        JsBlock bodyCopy = AstUtil.deepCopy(body);
        JsExpression conditionCopy = AstUtil.deepCopy(condition);
        JsParameter paramCopy = AstUtil.deepCopy(param);

        return new JsCatch(scopeCopy, bodyCopy, conditionCopy, paramCopy).withMetadataFrom(this);
    }

    private JsCatch(JsCatchScope scope, JsBlock body, JsExpression condition, JsParameter param) {
        this.scope = scope;
        this.body = body;
        this.condition = condition;
        this.param = param;
    }
}
