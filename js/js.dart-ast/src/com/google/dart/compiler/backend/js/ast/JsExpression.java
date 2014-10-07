package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.NotNull;

public interface JsExpression extends JsNode {
    boolean isLeaf();

    @NotNull
    JsStatement makeStmt();

    @Override
    JsExpression source(Object info);

    @NotNull
    @Override
    JsExpression deepCopy();
}
