/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

public final class JsDoubleLiteral extends JsNumberLiteral {
    public final double value;

    public JsDoubleLiteral(double value) {
        this.value = value;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitDouble(this);
    }

    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsDoubleLiteral deepCopy() {
        return AbstractNodeKt.withMetadataFrom(new JsDoubleLiteral(value), this);
    }
}
