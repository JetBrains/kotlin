/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

public final class JsThisRef extends JsAssignableExpression {
    // `this` is a leaf primary expression: it is side-effect-free and never needs parentheses.
    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitThis(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsThisRef deepCopy() {
        return AbstractNodeKt.withMetadataFrom(new JsThisRef(), this);
    }
}
