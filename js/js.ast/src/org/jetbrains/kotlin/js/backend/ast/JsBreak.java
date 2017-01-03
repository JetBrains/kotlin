// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the JavaScript break statement.
 */
public final class JsBreak extends JsContinue {
    public JsBreak() {
        super(null);
    }

    public JsBreak(JsNameRef label) {
        super(label);
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitBreak(this);
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
    public JsBreak deepCopy() {
        return new JsBreak(AstUtil.deepCopy(label)).withMetadataFrom(this);
    }
}
