// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.util;

import com.google.dart.compiler.backend.js.ast.*;

/**
 * @author johnlenz@google.com (John Lenz)
 */
public final class AstUtil {
    private AstUtil() {
    }

    public static JsNameRef newQualifiedNameRef(String name) {
        JsNameRef node = null;
        int endPos;
        int startPos = 0;
        do {
            endPos = name.indexOf('.', startPos);
            String part = (endPos == -1
                           ? name.substring(startPos)
                           : name.substring(startPos, endPos));
            node = new JsNameRef(part, node);
            startPos = endPos + 1;
        }
        while (endPos != -1);

        return node;
    }

    /**
     * Returns a sequence of expressions (using the binary sequence operator).
     *
     * @param exprs - expressions to add to sequence
     * @return a sequence of expressions.
     */
    public static JsBinaryOperation newSequence(JsExpression... exprs) {
        if (exprs.length < 2) {
            throw new RuntimeException("newSequence expects at least two arguments");
        }
        JsExpression result = exprs[exprs.length - 1];
        for (int i = exprs.length - 2; i >= 0; i--) {
            result = new JsBinaryOperation(JsBinaryOperator.COMMA, exprs[i], result);
        }
        return (JsBinaryOperation) result;
    }
}
