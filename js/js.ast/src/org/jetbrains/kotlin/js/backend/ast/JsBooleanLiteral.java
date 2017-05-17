/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        return new JsBooleanLiteral(value).withMetadataFrom(this);
    }
}
