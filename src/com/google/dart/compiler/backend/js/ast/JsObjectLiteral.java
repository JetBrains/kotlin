// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.intellij.util.SmartList;

import java.util.List;

public final class JsObjectLiteral extends JsLiteral {
    private final List<JsPropertyInitializer> properties;

    private final boolean multiline;

    public JsObjectLiteral() {
        this(new SmartList<JsPropertyInitializer>());
    }

    public JsObjectLiteral(boolean multiline) {
        this(new SmartList<JsPropertyInitializer>(), multiline);
    }

    public boolean isMultiline() {
        return multiline;
    }

    public JsObjectLiteral(List<JsPropertyInitializer> properties) {
        this(properties, false);
    }

    public JsObjectLiteral(List<JsPropertyInitializer> properties, boolean multiline) {
        this.properties = properties;
        this.multiline = multiline;
    }

    public List<JsPropertyInitializer> getPropertyInitializers() {
        return properties;
    }

    @Override
    public boolean hasSideEffects() {
        for (JsPropertyInitializer prop : properties) {
            if (prop.hasSideEffects()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBooleanFalse() {
        return false;
    }

    @Override
    public boolean isBooleanTrue() {
        return true;
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return true;
    }

    @Override
    public boolean isDefinitelyNull() {
        return false;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            v.acceptWithInsertRemove(properties);
        }
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.OBJECT;
    }
}
