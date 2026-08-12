/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.JsFunction;
import org.jetbrains.kotlin.js.backend.ast.JsNode;
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper;
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil;
import org.jetbrains.kotlin.js.testOld.utils.DirectiveTestUtils;

import java.io.File;
import java.util.List;

import static kotlin.test.AssertionsKt.assertTrue;
import static org.jetbrains.kotlin.js.inline.util.CollectUtilsKt.collectInstances;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CountNodesDirective<T extends JsNode> extends DirectiveTestUtils.DirectiveHandler {

    @NotNull
    private final Class<T> klass;

    public CountNodesDirective(@NotNull Class<T> klass) {
        super();
        this.klass = klass;
    }

    @Override
    public void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
        String functionName = arguments.getNamedArgument("function");
        String countStr = arguments.findNamedArgument("count");
        String maxCountStr = arguments.findNamedArgument("max");
        String includeNestedDeclarations = arguments.findNamedArgument("includeNestedDeclarations");

        JsFunction function = AstSearchUtil.getFunction(ast, functionName);
        List<T> nodes =
                collectInstances(klass, function.getBody(), includeNestedDeclarations != null && includeNestedDeclarations.equals("true"));
        int actualCount = 0;

        for (T node : nodes) {
            actualCount += getActualCountFor(node, arguments);
        }

        if (countStr != null) {
            int expectedCount = Integer.valueOf(countStr);

            String message = "Function " + functionName + " contains " + actualCount +
                             " nodes of type " + klass.getName() +
                             ", but expected count is " + expectedCount;
            assertEquals(expectedCount, actualCount, message);
        }
        else if (maxCountStr != null) {
            int expectedCount = Integer.valueOf(maxCountStr);

            String message = "Function " + functionName + " contains " + actualCount +
                             " nodes of type " + klass.getName() +
                             ", but expected max is " + expectedCount;
            assertTrue(expectedCount >= actualCount, message);
        }
        else {
            throw new IllegalArgumentException("'max' or 'count' argument should be provided");
        }
    }

    protected int getActualCountFor(@NotNull T node, @NotNull ArgumentsHelper arguments) {
        return 1;
    }
}
