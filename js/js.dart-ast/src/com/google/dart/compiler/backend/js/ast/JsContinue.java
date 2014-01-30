// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.Nullable;

public class JsContinue extends SourceInfoAwareJsNode implements JsStatement {
    protected final String label;

    public JsContinue() {
        this(null);
    }

    public JsContinue(@Nullable String label) {
        super();
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitContinue(this);
    }
}
