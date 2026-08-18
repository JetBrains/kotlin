/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

public final class JsBooleanLiteral extends JsLiteral.JsValueLiteral {
    private final boolean value;

    public JsBooleanLiteral(boolean value) {
        this.value = value;
    }

    public static boolean isTrue(@NotNull JsExpression expression) {
        return expression instanceof JsBooleanLiteral && ((JsBooleanLiteral) expression).getValue();
    }

    public static boolean isFalse(@NotNull JsExpression expression) {
        return expression instanceof JsBooleanLiteral && !((JsBooleanLiteral) expression).getValue();
    }

    public boolean getValue() {
    return value;
  }

    @Override
    public void accept(JsVisitor v) {
        v.visitBoolean(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsBooleanLiteral deepCopy() {
        return AbstractNodeKt.withMetadataFrom(new JsBooleanLiteral(value), this);
    }
}
