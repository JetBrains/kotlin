// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.NotNull;

public class JsEmpty extends SourceInfoAwareJsNode implements JsStatement {
    JsEmpty() {
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitEmpty(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsEmpty deepCopy() {
        return this;
    }
}
