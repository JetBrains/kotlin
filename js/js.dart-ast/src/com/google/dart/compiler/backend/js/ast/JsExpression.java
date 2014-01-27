package com.google.dart.compiler.backend.js.ast;

public interface JsExpression extends JsNode {
    boolean isLeaf();

    JsStatement makeStmt();

    @Override
    JsExpression source(Object info);
}
