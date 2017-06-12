// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

/**
 * A JavaScript program.
 */
public final class JsProgram extends SourceInfoAwareJsNode {
    private final JsGlobalBlock globalBlock = new JsGlobalBlock();

    private final JsRootScope rootScope;
    private final JsObjectScope topScope;

    public JsProgram() {
        rootScope = new JsRootScope(this);
        topScope = new JsObjectScope(rootScope, "Global");
    }

    public JsGlobalBlock getGlobalBlock() {
        return globalBlock;
    }

    /**
     * Gets the quasi-mythical root scope. This is not the same as the top scope;
     * all unresolvable identifiers wind up here, because they are considered
     * external to the program.
     */
    public JsRootScope getRootScope() {
        return rootScope;
    }

    /**
     * Gets the top level scope. This is the scope of all the statements in the
     * main program.
     */
    public JsObjectScope getScope() {
        return topScope;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitProgram(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(globalBlock);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            v.accept(globalBlock);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsProgram deepCopy() {
        throw new UnsupportedOperationException();
    }
}
