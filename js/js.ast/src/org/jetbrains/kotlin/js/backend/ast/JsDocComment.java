/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class JsDocComment extends JsExpression {
    private final Map<String, Object> tags;

    public JsDocComment(Map<String, Object> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitDocComment(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
    }

    @NotNull
    @Override
    public JsDocComment deepCopy() {
        return AbstractNodeKt.withMetadataFrom(new JsDocComment(tags), this);
    }
}
