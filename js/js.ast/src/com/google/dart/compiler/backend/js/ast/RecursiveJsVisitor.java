package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.NotNull;

public abstract class RecursiveJsVisitor extends JsVisitor {
    @Override
    protected void visitElement(@NotNull JsNode node) {
        node.acceptChildren(this);
    }
}
