// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a JavaScript do..while statement.
 */
public class JsDoWhile extends JsWhile {
    public JsDoWhile() {
    }

    public JsDoWhile(JsExpression condition, JsStatement body) {
        super(condition, body);
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitDoWhile(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            body = v.acceptStatement(body);
            condition = v.accept(condition);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsDoWhile deepCopy() {
        JsExpression conditionCopy = AstUtil.deepCopy(condition);
        JsStatement bodyCopy = AstUtil.deepCopy(body);

        return new JsDoWhile(conditionCopy, bodyCopy).withMetadataFrom(this);
    }
}
