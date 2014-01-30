// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.intellij.util.SmartList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A JavaScript <code>try</code> statement.
 */
public class JsTry extends SourceInfoAwareJsNode implements JsStatement {
    private final List<JsCatch> catches;
    private JsBlock finallyBlock;
    private JsBlock tryBlock;

    public JsTry() {
        catches = new SmartList<JsCatch>();
    }

    public JsTry(JsBlock tryBlock, List<JsCatch> catches, @Nullable JsBlock finallyBlock) {
        this.tryBlock = tryBlock;
        this.catches = catches;
        this.finallyBlock = finallyBlock;
    }

    public List<JsCatch> getCatches() {
        return catches;
    }

    public JsBlock getFinallyBlock() {
        return finallyBlock;
    }

    public JsBlock getTryBlock() {
        return tryBlock;
    }

    public void setFinallyBlock(JsBlock block) {
        finallyBlock = block;
    }

    public void setTryBlock(JsBlock block) {
        tryBlock = block;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitTry(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(tryBlock);
        visitor.acceptWithInsertRemove(catches);
        if (finallyBlock != null) {
           visitor.accept(finallyBlock);
        }
    }
}
