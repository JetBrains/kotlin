// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

public final class JsRegExp extends JsLiteral.JsValueLiteral {
    private String flags;
    private String pattern;

    public JsRegExp() {
    }

    public String getFlags() {
        return flags;
    }

    public String getPattern() {
        return pattern;
    }

    public void setFlags(String suffix) {
        flags = suffix;
    }

    public void setPattern(String re) {
        pattern = re;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitRegExp(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        v.visit(this, ctx);
        v.endVisit(this, ctx);
    }
}
