// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the JavaScript case statement.
 */
public final class JsCase extends JsSwitchMember {
    private JsExpression caseExpression;

    public JsCase() {
        super();
    }

    public JsExpression getCaseExpression() {
        return caseExpression;
    }

    public void setCaseExpression(JsExpression caseExpression) {
        this.caseExpression = caseExpression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitCase(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(caseExpression);
        super.acceptChildren(visitor);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            caseExpression = v.accept(caseExpression);
            v.acceptStatementList(statements);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsCase deepCopy() {
        JsCase caseCopy = new JsCase();
        caseCopy.caseExpression = AstUtil.deepCopy(caseExpression);
        caseCopy.statements.addAll(AstUtil.deepCopy(statements));

        return caseCopy.withMetadataFrom(this);
    }
}
