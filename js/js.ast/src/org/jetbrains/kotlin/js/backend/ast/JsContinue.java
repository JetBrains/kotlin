// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsContinue extends SourceInfoAwareJsNode implements JsStatement {
    protected JsNameRef label;

    public JsContinue() {
        this(null);
    }

    public JsContinue(@Nullable JsNameRef label) {
        super();
        this.label = label;
    }

    public JsNameRef getLabel() {
        return label;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitContinue(this);
    }

    @Override
    public void acceptChildren(JsVisitor v) {
        if (label != null){
            v.accept(label);
        }
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            if (label != null){
                label = v.accept(label);
            }
        }

        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsContinue deepCopy() {
        if (label == null) return new JsContinue();

        return new JsContinue(AstUtil.deepCopy(label)).withMetadataFrom(this);
    }
}
