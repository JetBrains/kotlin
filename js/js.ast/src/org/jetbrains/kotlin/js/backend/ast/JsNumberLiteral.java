// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

public abstract class JsNumberLiteral extends JsLiteral.JsValueLiteral {
    public static final JsIntLiteral ZERO = new JsIntLiteral(0);

    public static final class JsDoubleLiteral extends JsNumberLiteral {
        public final double value;

        JsDoubleLiteral(double value) {
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
    }

    public static final class JsIntLiteral extends JsNumberLiteral {
        public final int value;

        JsIntLiteral(int value) {
            this.value = value;
        }

        @Override
        public void accept(JsVisitor v) {
            v.visitInt(this);
        }

        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public void traverse(JsVisitorWithContext v, JsContext ctx) {
            v.visit(this, ctx);
            v.endVisit(this, ctx);
        }
    }
}
