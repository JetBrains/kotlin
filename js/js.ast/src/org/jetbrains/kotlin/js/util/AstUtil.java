// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.util;

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.JsNode;

import java.util.ArrayList;
import java.util.List;

public final class AstUtil {
    private AstUtil() {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends JsNode> T deepCopy(@Nullable T node) {
        if (node == null) return null;

        return (T) node.deepCopy();
    }

    @NotNull
    public static <T extends JsNode> List<T> deepCopy(@Nullable List<T> nodes) {
        if (nodes == null) return new SmartList<>();

        List<T> nodesCopy = new ArrayList<>(nodes.size());
        for (T node : nodes) {
            nodesCopy.add(deepCopy(node));
        }

        return nodesCopy;
    }
}
