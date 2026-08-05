/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

public final class JsSuperRef extends JsLiteral.JsValueLiteral {
    @Override
    public void accept(JsVisitor v) {
        v.visitSuper(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsSuperRef deepCopy() {
        return AbstractNodeKt.withMetadataFrom(new JsSuperRef(), this);
    }
}
