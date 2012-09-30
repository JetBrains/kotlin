// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript string literal expression.
 */
public abstract class JsValueLiteral extends JsLiteral {
    protected JsValueLiteral() {
    }

    @Override
    public final boolean hasSideEffects() {
        return false;
    }

    @Override
    public final boolean isLeaf() {
        return true;
    }
}
