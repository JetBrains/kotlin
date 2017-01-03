// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a JavaScript if statement.
 */
public final class JsIf extends SourceInfoAwareJsNode implements JsStatement {
    @NotNull
    private JsExpression ifExpression;

    @NotNull
    private JsStatement thenStatement;

    @Nullable
    private JsStatement elseStatement;

    public JsIf(@NotNull JsExpression ifExpression, @NotNull JsStatement thenStatement, @Nullable JsStatement elseStatement) {
        this.ifExpression = ifExpression;
        this.thenStatement = thenStatement;
        this.elseStatement = elseStatement;
    }

    public JsIf(@NotNull JsExpression ifExpression, @NotNull JsStatement thenStatement) {
        this(ifExpression, thenStatement, null);
    }

    @Nullable
    public JsStatement getElseStatement() {
        return elseStatement;
    }

    @NotNull
    public JsExpression getIfExpression() {
        return ifExpression;
    }

    @NotNull
    public JsStatement getThenStatement() {
        return thenStatement;
    }

    public void setElseStatement(@Nullable JsStatement elseStatement) {
        this.elseStatement = elseStatement;
    }

    public void setIfExpression(@NotNull JsExpression ifExpression) {
        this.ifExpression = ifExpression;
    }

    public void setThenStatement(@NotNull JsStatement thenStatement) {
        this.thenStatement = thenStatement;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitIf(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(ifExpression);
        visitor.accept(thenStatement);
        if (elseStatement != null) {
            visitor.accept(elseStatement);
        }
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            ifExpression = v.accept(ifExpression);
            thenStatement = v.acceptStatement(thenStatement);
            if (elseStatement != null) {
                elseStatement = v.acceptStatement(elseStatement);
            }
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsIf deepCopy() {
        JsExpression ifCopy = AstUtil.deepCopy(ifExpression);
        JsStatement thenCopy = AstUtil.deepCopy(thenStatement);
        JsStatement elseCopy = AstUtil.deepCopy(elseStatement);

        return new JsIf(ifCopy, thenCopy, elseCopy).withMetadataFrom(this);
    }
}
