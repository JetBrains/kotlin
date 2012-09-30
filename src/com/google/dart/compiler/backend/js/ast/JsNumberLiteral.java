// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

public abstract class JsNumberLiteral extends JsValueLiteral {
    @Override
    public boolean isDefinitelyNotNull() {
        return true;
    }

    @Override
    public boolean isDefinitelyNull() {
        return false;
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.NUMBER;
    }

    public static final class JsDoubleLiteral extends JsNumberLiteral {
        public final double value;

        JsDoubleLiteral(double value) {
            this.value = value;
        }

        @Override
        public boolean isBooleanFalse() {
            return value == 0.0;
        }

        @Override
        public boolean isBooleanTrue() {
            return value != 0.0;
        }

        @Override
        public void traverse(JsVisitor v, JsContext ctx) {
            v.visit(this, ctx);
            v.endVisit(this, ctx);
        }

        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class JsIntLiteral extends JsNumberLiteral {
        public final int value;

        JsIntLiteral(int value) {
            this.value = value;
        }

        @Override
        public boolean isBooleanFalse() {
            return value == 0;
        }

        @Override
        public boolean isBooleanTrue() {
            return value != 0;
        }

        @Override
        public void traverse(JsVisitor v, JsContext ctx) {
            v.visit(this, ctx);
            v.endVisit(this, ctx);
        }

        public String toString() {
            return String.valueOf(value);
        }
    }
}
