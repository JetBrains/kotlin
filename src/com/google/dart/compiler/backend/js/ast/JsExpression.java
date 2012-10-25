package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.HasSourceInfo;

public interface JsExpression extends JsNode, HasSourceInfo {
    boolean isLeaf();

    JsStatement makeStmt();
}
