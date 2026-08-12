/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.JsNode;
import org.jetbrains.kotlin.js.test.ast.JsAstDirectives;
import org.jetbrains.kotlin.js.test.ast.directives.JsAstDirective;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives;
import org.jetbrains.kotlin.test.directives.model.ValueDirective;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DirectiveTestUtils {

    private DirectiveTestUtils() {}

    private static final List<ValueDirective<? extends JsAstDirective>> DIRECTIVE_HANDLERS = Arrays.asList(
            JsAstDirectives.INSTANCE.getEXPECT_GENERATED_JS(),
            JsAstDirectives.INSTANCE.getCHECK_CONTAINS_NO_CALLS(),
            JsAstDirectives.INSTANCE.getCHECK_NOT_CALLED(),
            JsAstDirectives.INSTANCE.getFUNCTION_CALLED_TIMES(),
            JsAstDirectives.INSTANCE.getPROPERTY_NOT_USED(),
            JsAstDirectives.INSTANCE.getPROPERTY_NOT_READ_FROM(),
            JsAstDirectives.INSTANCE.getPROPERTY_NOT_WRITTEN_TO(),
            JsAstDirectives.INSTANCE.getPROPERTY_READ_COUNT(),
            JsAstDirectives.INSTANCE.getPROPERTY_WRITE_COUNT(),
            JsAstDirectives.INSTANCE.getCHECK_CLASS_EXISTS(),
            JsAstDirectives.INSTANCE.getCHECK_FUNCTION_EXISTS(),
            JsAstDirectives.INSTANCE.getCHECK_CALLED_IN_SCOPE(),
            JsAstDirectives.INSTANCE.getCHECK_NOT_CALLED_IN_SCOPE(),
            JsAstDirectives.INSTANCE.getCHECK_COMMENT_EXISTS(),
            JsAstDirectives.INSTANCE.getCHECK_LABELS_COUNT(),
            JsAstDirectives.INSTANCE.getCHECK_VARS_COUNT(),
            JsAstDirectives.INSTANCE.getCHECK_BREAKS_COUNT(),
            JsAstDirectives.INSTANCE.getCHECK_NULLS_COUNT(),
            JsAstDirectives.INSTANCE.getCHECK_NEW_COUNT(),
            JsAstDirectives.INSTANCE.getCHECK_CASES_COUNT(),
            JsAstDirectives.INSTANCE.getCHECK_IF_COUNT(),
            JsAstDirectives.INSTANCE.getCHECK_SUPER_COUNT(),
            JsAstDirectives.INSTANCE.getCHECK_STRING_LITERAL_COUNT(),
            JsAstDirectives.INSTANCE.getHAS_NO_CAPTURED_VARS()
    );

    public static void processDirectives(
            @NotNull JsNode ast,
            @NotNull File sourceFile,
            @NotNull TargetBackend targetBackend,
            @NotNull RegisteredDirectives allDirectives
    ) {
        Assertions.assertAll(DIRECTIVE_HANDLERS.stream().map((directive -> () -> {
            for (JsAstDirective entry : allDirectives.get(directive)) {
                if (entry.shouldRunWithBackend(targetBackend)) {
                    entry.evaluate(ast, sourceFile);
                }
            }
        })));
    }

    @NotNull
    public static JsNode findScope(@NotNull JsNode node, @Nullable String scopeFunctionName) {
        if (scopeFunctionName != null) {
            return AstSearchUtil.getFunction(node, scopeFunctionName);
        }
        return node;
    }
}
