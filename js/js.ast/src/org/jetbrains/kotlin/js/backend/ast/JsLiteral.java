// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

public abstract class JsLiteral extends JsExpression {
    /**
     * A JavaScript string literal expression.
     */
    public abstract static class JsValueLiteral extends JsLiteral {
        protected JsValueLiteral() {
        }

        @Override
        public final boolean isLeaf() {
            return true;
        }

        @NotNull
        @Override
        public JsExpression deepCopy() {
            return this;
        }
    }

    public static boolean isTrueBoolean(@NotNull JsExpression expression) {
        return expression instanceof JsBooleanLiteral && ((JsBooleanLiteral) expression).getValue();
    }

    public static boolean isFalseBoolean(@NotNull JsExpression expression) {
        return expression instanceof JsBooleanLiteral && !((JsBooleanLiteral) expression).getValue();
    }
}
