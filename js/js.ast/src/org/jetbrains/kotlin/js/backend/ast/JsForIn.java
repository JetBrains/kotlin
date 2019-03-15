// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

public class JsForIn extends SourceInfoAwareJsNode implements JsLoop {
    private JsStatement body;
    private JsExpression iterExpression;
    private JsExpression objectExpression;

    // Optional: the name of a new iterator variable to introduce
    private final JsName iterVarName;

    public JsForIn() {
        this(null);
    }

    public JsForIn(JsName iterVarName) {
        this.iterVarName = iterVarName;
    }

    public JsForIn(JsName iterVarName, JsExpression iterExpression, JsExpression objectExpression, JsStatement body) {

        this.iterVarName = iterVarName;
        this.iterExpression = iterExpression;
        this.objectExpression = objectExpression;
        this.body = body;
    }

    public JsStatement getBody() {
        return body;
    }

    public JsExpression getIterExpression() {
        return iterExpression;
    }

    public JsName getIterVarName() {
        return iterVarName;
    }

    public JsExpression getObjectExpression() {
        return objectExpression;
    }

    public void setBody(JsStatement body) {
        this.body = body;
    }

    public void setIterExpression(JsExpression iterExpression) {
        this.iterExpression = iterExpression;
    }

    public void setObjectExpression(JsExpression objectExpression) {
        this.objectExpression = objectExpression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitForIn(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        if (iterExpression != null) {
            visitor.acceptLvalue(iterExpression);
        }
        visitor.accept(objectExpression);
        visitor.accept(body);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            if (iterExpression != null) {
                iterExpression = v.acceptLvalue(iterExpression);
            }
            objectExpression = v.accept(objectExpression);
            body = v.acceptStatement(body);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsForIn deepCopy() {
        JsStatement bodyCopy = AstUtil.deepCopy(body);
        JsExpression iterCopy = AstUtil.deepCopy(iterExpression);
        JsExpression objectCopy = AstUtil.deepCopy(objectExpression);

        return new JsForIn(iterVarName, iterCopy, objectCopy, bodyCopy).withMetadataFrom(this);
    }
}
