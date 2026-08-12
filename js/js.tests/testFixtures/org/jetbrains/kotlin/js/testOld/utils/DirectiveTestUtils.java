/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.utils;

import com.intellij.openapi.util.text.StringUtil;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.inline.util.CollectUtilsKt;
import org.jetbrains.kotlin.js.test.ast.JsAstDirectives;
import org.jetbrains.kotlin.js.test.ast.directives.JsAstDirective;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives;
import org.jetbrains.kotlin.test.directives.model.ValueDirective;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static kotlin.test.AssertionsKt.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DirectiveTestUtils {

    private DirectiveTestUtils() {}

    private static final DirectiveHandler NOT_REFERENCED = new DirectiveHandler() {
        @Override
        public void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            String reference = arguments.getPositionalArgument(0);

            JsVisitor visitor = new RecursiveJsVisitor() {
                @Override
                public void visitNameRef(@NotNull JsNameRef nameRef) {
                    assertNotEquals(reference, nameRef.toString());
                }
            };

            visitor.accept(ast);
        }
    };

    private static final DirectiveHandler HAS_NO_CAPTURED_VARS = new DirectiveHandler() {
        @Override
        public void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            String functionName = arguments.getNamedArgument("function");

            Set<String> except = new HashSet<>();
            String exceptString = arguments.findNamedArgument("except");
            if (exceptString != null) {
                for (String exceptId : StringUtil.split(exceptString, ";")) {
                    except.add(exceptId.trim());
                }
            }

            JsFunction function = AstSearchUtil.getFunction(ast, functionName);
            Set<JsName> freeVars = CollectUtilsKt.collectFreeVariables(function);
            for (JsName freeVar : freeVars) {
                assertTrue(except.contains(freeVar.getIdent()),
                           "Function " + functionName + " captures free variable " + freeVar.getIdent());
            }
        }
    };

    private static final List<Pair<ValueDirective<? extends ArgumentsHelper>, DirectiveHandler>> DIRECTIVE_HANDLERS = Arrays.asList(
            new Pair<>(JsAstDirectives.INSTANCE.getEXPECT_GENERATED_JS(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_CONTAINS_NO_CALLS(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_NOT_CALLED(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getFUNCTION_CALLED_TIMES(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_NOT_USED(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_NOT_READ_FROM(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_NOT_WRITTEN_TO(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_READ_COUNT(),  new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_WRITE_COUNT(),  new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_CLASS_EXISTS(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_FUNCTION_EXISTS(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_CALLED_IN_SCOPE(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_NOT_CALLED_IN_SCOPE(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_COMMENT_EXISTS(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_LABELS_COUNT(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_VARS_COUNT(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_BREAKS_COUNT(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_NULLS_COUNT(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_NEW_COUNT(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_CASES_COUNT(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_IF_COUNT(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_SUPER_COUNT(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_STRING_LITERAL_COUNT(), new DirectiveHandler<>()),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_NOT_REFERENCED(), NOT_REFERENCED),
            new Pair<>(JsAstDirectives.INSTANCE.getHAS_NO_CAPTURED_VARS(), HAS_NO_CAPTURED_VARS)
    );

    @SuppressWarnings("unchecked")
    public static void processDirectives(
            @NotNull JsNode ast,
            @NotNull File sourceFile,
            @NotNull TargetBackend targetBackend,
            @NotNull RegisteredDirectives allDirectives
    ) {
        Assertions.assertAll(DIRECTIVE_HANDLERS.stream().map((directiveAndHandler -> {
            List<ArgumentsHelper> directiveEntries = (List<ArgumentsHelper>) allDirectives.get(directiveAndHandler.getFirst());
            return () -> directiveAndHandler.getSecond().process(ast, sourceFile, directiveEntries, targetBackend);
        })));
    }

    @NotNull
    public static JsNode findScope(@NotNull JsNode node, @Nullable String scopeFunctionName) {
        if (scopeFunctionName != null) {
            return AstSearchUtil.getFunction(node, scopeFunctionName);
        }
        return node;
    }

    public static class DirectiveHandler<A extends ArgumentsHelper> {

        public DirectiveHandler() {
        }

        /**
         * Processes directive entries.
         *
         * Each entry is expected to have the following format:
         * `// DIRECTIVE: arguments
         *
         * @see ArgumentsHelper for arguments format
         */
        public void process(@NotNull JsNode ast,
                @NotNull File sourceFile,
                @NotNull List<A> directiveEntries,
                @NotNull TargetBackend targetBackend
        ) throws Exception {
            for (A arguments : directiveEntries) {
                if (arguments.shouldRunWithBackend(targetBackend)) {
                    processEntry(ast, arguments, sourceFile);
                }
            }
        }

        public void processEntry(@NotNull JsNode ast, @NotNull A arguments, File sourceFile) throws Exception {
            if (arguments instanceof JsAstDirective) {
                ((JsAstDirective) arguments).evaluate(ast, sourceFile);
            }
        }
    }
}
