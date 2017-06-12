// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

public final class JsNullLiteral extends JsLiteral.JsValueLiteral {
    public JsNullLiteral() {
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitNull(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsNullLiteral deepCopy() {
        return new JsNullLiteral().withMetadataFrom(this);
    }
}
