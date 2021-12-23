/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

public class JsCode extends SourceInfoAwareJsNode implements JsStatement {
    private String value;

    public JsCode(String code) {
        this.value = code;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void accept(JsVisitor v) { v.visitCode(this); }

    @Override
    public void acceptChildren(JsVisitor visitor) {}

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {}

    @NotNull
    @Override
    public JsCode deepCopy() {
        return new JsCode(value);
    }
}
