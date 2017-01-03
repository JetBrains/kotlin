// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
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

    public JsTry(JsBlock tryBlock, @Nullable JsCatch jsCatch, @Nullable JsBlock finallyBlock) {
        this(tryBlock, new SmartList<JsCatch>(), finallyBlock);

        if (jsCatch != null) {
            catches.add(jsCatch);
        }
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

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            tryBlock = v.acceptStatement(tryBlock);
            v.acceptList(catches);
            if (finallyBlock != null) {
                finallyBlock = v.acceptStatement(finallyBlock);
            }
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsTry deepCopy() {
        JsBlock tryCopy = AstUtil.deepCopy(tryBlock);
        List<JsCatch> catchCopy = AstUtil.deepCopy(catches);
        JsBlock finallyCopy = AstUtil.deepCopy(finallyBlock);

        return new JsTry(tryCopy, catchCopy, finallyCopy).withMetadataFrom(this);
    }
}
