// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a JavaScript expression for array literals.
 */
public final class JsArrayLiteral extends JsLiteral {
    private final List<JsExpression> expressions;

    public JsArrayLiteral() {
        expressions = new SmartList<JsExpression>();
    }

    public JsArrayLiteral(List<JsExpression> expressions) {
        this.expressions = expressions;
    }

    public List<JsExpression> getExpressions() {
        return expressions;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitArray(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.acceptWithInsertRemove(expressions);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            v.acceptList(expressions);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsArrayLiteral deepCopy() {
        return new JsArrayLiteral(AstUtil.deepCopy(expressions)).withMetadataFrom(this);
    }
}
