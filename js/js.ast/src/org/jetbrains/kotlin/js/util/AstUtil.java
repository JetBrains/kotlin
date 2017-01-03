// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.util;

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation;
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsNode;

import java.util.ArrayList;
import java.util.List;

public final class AstUtil {
    private AstUtil() {
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

    @Nullable
    public static <T extends JsNode> T deepCopy(@Nullable T node) {
        if (node == null) return null;

        //noinspection unchecked
        return (T) node.deepCopy();
    }

    @NotNull
    public static <T extends JsNode> List<T> deepCopy(@Nullable List<T> nodes) {
        if (nodes == null) return new SmartList<T>();

        List<T> nodesCopy = new ArrayList<T>(nodes.size());
        for (T node : nodes) {
            nodesCopy.add(deepCopy(node));
        }

        return nodesCopy;
    }
}
