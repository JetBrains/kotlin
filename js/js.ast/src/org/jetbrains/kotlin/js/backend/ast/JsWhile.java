// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * A JavaScript <code>while</code> statement.
 */
public class JsWhile extends SourceInfoAwareJsNode implements JsLoop {
    protected JsStatement body;
    protected JsExpression condition;

    public JsWhile() {
    }

    public JsWhile(JsExpression condition, JsStatement body) {
        this.condition = condition;
        this.body = body;
    }

    public JsStatement getBody() {
        return body;
    }

    public JsExpression getCondition() {
        return condition;
    }

    public void setBody(JsStatement body) {
        this.body = body;
    }

    public void setCondition(JsExpression condition) {
        this.condition = condition;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitWhile(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(condition);
        visitor.accept(body);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            condition = v.accept(condition);
            body = v.acceptStatement(body);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsWhile deepCopy() {
        JsExpression conditionCopy = AstUtil.deepCopy(condition);
        JsStatement bodyCopy = AstUtil.deepCopy(body);

        return new JsWhile(conditionCopy, bodyCopy).withMetadataFrom(this);
    }
}
