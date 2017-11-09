// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a JavaScript debugger statement.
 */
public class JsDebugger extends SourceInfoAwareJsNode implements JsStatement {
    public JsDebugger() {
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitDebugger(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {

    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsDebugger deepCopy() {
        return this;
    }
}
