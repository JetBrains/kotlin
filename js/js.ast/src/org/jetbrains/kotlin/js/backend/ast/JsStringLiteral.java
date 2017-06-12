// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

public final class JsStringLiteral extends JsLiteral.JsValueLiteral {
    private final String value;

    public JsStringLiteral(String value) {
        this.value = value;
    }

    public String getValue() {
    return value;
  }

    @Override
    public void accept(JsVisitor v) {
    v.visitString(this);
  }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsStringLiteral deepCopy() {
        return new JsStringLiteral(value).withMetadataFrom(this);
    }
}
