package com.google.dart.compiler.backend.js.ast;

public abstract class RecursiveJsVisitor extends JsVisitor {
    @Override
    protected void visitElement(JsNode node, JsContext context) {
        node.acceptChildren(this, context);
    }
}
