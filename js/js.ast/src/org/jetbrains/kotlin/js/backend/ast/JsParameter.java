// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.common.Symbol;
import org.jetbrains.annotations.NotNull;

/**
 * A JavaScript parameter.
 */
public final class JsParameter extends SourceInfoAwareJsNode implements HasName {
    @NotNull
    private JsName name;

    public JsParameter(@NotNull JsName name) {
        this.name = name;
    }

    @Override
    @NotNull
    public JsName getName() {
        return name;
    }

    @Override
    public void setName(@NotNull JsName name) {
        this.name = name;
    }

    @Override
    @NotNull
    public Symbol getSymbol() {
        return name;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitParameter(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsParameter deepCopy() {
        return new JsParameter(name).withMetadataFrom(this);
    }
}
