// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

/**
 * One independently loadable fragment of a {@link JsProgram}.
 */
public class JsProgramFragment extends SourceInfoAwareJsNode {
    private final JsGlobalBlock globalBlock;

    public JsProgramFragment() {
        globalBlock = new JsGlobalBlock();
    }

    public JsBlock getGlobalBlock() {
        return globalBlock;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitProgramFragment(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(globalBlock);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            v.acceptStatement(globalBlock);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsProgramFragment deepCopy() {
        throw new UnsupportedOperationException();
    }
}
