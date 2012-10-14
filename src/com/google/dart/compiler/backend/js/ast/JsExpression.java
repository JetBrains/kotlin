package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.HasSourceInfo;

public interface JsExpression extends JsNode, HasSourceInfo, JsVisitable {
    /**
     * Determines whether the expression can cause side effects.
     */
    boolean hasSideEffects();

    /**
     * True if the target expression is definitely not null.
     */
    boolean isDefinitelyNotNull();

    /**
     * True if the target expression is definitely null.
     */
    boolean isDefinitelyNull();

    boolean isLeaf();

    JsStatement makeStmt();
}
