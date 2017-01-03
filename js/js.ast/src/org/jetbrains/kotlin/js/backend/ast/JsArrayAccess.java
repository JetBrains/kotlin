// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a javascript expression for array access.
 */
public final class JsArrayAccess extends JsExpression {
    private JsExpression arrayExpression;
    private JsExpression indexExpression;

    public JsArrayAccess() {
        super();
    }

    public JsArrayAccess(JsExpression arrayExpression, JsExpression indexExpression) {
        this.arrayExpression = arrayExpression;
        this.indexExpression = indexExpression;
    }

    public JsExpression getArrayExpression() {
        return arrayExpression;
    }

    public JsExpression getIndexExpression() {
        return indexExpression;
    }

    public void setArrayExpression(JsExpression arrayExpression) {
        this.arrayExpression = arrayExpression;
    }

    public void setIndexExpression(JsExpression indexExpression) {
        this.indexExpression = indexExpression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitArrayAccess(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(arrayExpression);
        visitor.accept(indexExpression);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            arrayExpression = v.accept(arrayExpression);
            indexExpression = v.accept(indexExpression);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsArrayAccess deepCopy() {
        JsExpression arrayCopy = AstUtil.deepCopy(arrayExpression);
        JsExpression indexCopy = AstUtil.deepCopy(indexExpression);

        return new JsArrayAccess(arrayCopy, indexCopy).withMetadataFrom(this);
    }
}
