// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the default option in a JavaScript swtich statement.
 */
public final class JsDefault extends JsSwitchMember {
    @Override
    public void accept(JsVisitor v) {
        v.visitDefault(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            v.acceptStatementList(statements);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsDefault deepCopy() {
        JsDefault defaultCopy = new JsDefault();
        defaultCopy.statements.addAll(AstUtil.deepCopy(statements));
        return defaultCopy.withMetadataFrom(this);
    }
}
