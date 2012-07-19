// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

public final class JsNullLiteral extends JsValueLiteral {
    JsNullLiteral() {
    }

    @Override
    public boolean isBooleanFalse() {
        return true;
    }

    @Override
    public boolean isBooleanTrue() {
        return false;
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return false;
    }

    @Override
    public boolean isDefinitelyNull() {
        return true;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.NULL;
    }
}
