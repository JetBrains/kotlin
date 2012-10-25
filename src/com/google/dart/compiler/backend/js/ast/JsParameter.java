// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.Symbol;

/**
 * A JavaScript parameter.
 */
public final class JsParameter extends SourceInfoAwareJsNode implements HasName {
    private final JsName name;

    public JsParameter(JsName name) {
        this.name = name;
    }

    @Override
    public JsName getName() {
        return name;
    }

    @Override
    public Symbol getSymbol() {
        return name;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitParameter(this);
    }
}
