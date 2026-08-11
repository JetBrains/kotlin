/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.utils;

import com.intellij.openapi.util.text.StringUtil;
import kotlin.Pair;
import kotlin.text.StringsKt;
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

import static kotlin.test.AssertionsKt.assertFalse;
import static kotlin.test.AssertionsKt.assertTrue;
import static org.jetbrains.kotlin.js.inline.util.CollectUtilsKt.collectInstances;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DirectiveTestUtils {

    private DirectiveTestUtils() {}

    private static final DirectiveHandler FUNCTION_CALLED_IN_SCOPE = new DirectiveHandler() {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            // Be more restrictive, check qualified match by default
            checkCalledInScope(ast, arguments.getNamedArgument("function"), arguments.getNamedArgument("scope"),
                               parseBooleanArgument(arguments, "qualified", true));
        }
    };

    private static final DirectiveHandler FUNCTION_NOT_CALLED_IN_SCOPE = new DirectiveHandler() {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            // Be more restrictive, check unqualified match by default
            checkNotCalledInScope(ast, arguments.getNamedArgument("function"), arguments.getNamedArgument("scope"),
                                  parseBooleanArgument(arguments, "qualified", false));
        }
    };

    private static boolean parseBooleanArgument(@NotNull ArgumentsHelper arguments, @NotNull String name, boolean defaultValue) {
        String value = arguments.findNamedArgument(name);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    private static abstract class NodeExistenceDirective extends DirectiveHandler {
        private boolean isElementExists = false;
        private boolean shouldCheckForExistence;

        NodeExistenceDirective(@NotNull String directive, boolean shouldCheckForExistence) {
            super();
            this.shouldCheckForExistence = shouldCheckForExistence;
        }

        protected abstract String getTextForError();
        protected abstract JsVisitor getJsVisitorForElement();
        protected abstract void loadArguments(@NotNull ArgumentsHelper arguments);

        protected void setElementExists(boolean isElementExists) {
            this.isElementExists = isElementExists;
        }

        protected boolean isElementExists() {
            return isElementExists;
        }

        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            loadArguments(arguments);
            getJsVisitorForElement().accept(ast);
            assertExistence();
            setElementExists(false);
        }

        private void assertExistence() {
            String message = getTextForError();
            if (shouldCheckForExistence) {
                assertTrue(isElementExists, message);
            } else {
                assertFalse(isElementExists, message);
            }
        }
    }

    private static class CountNodesDirective<T extends JsNode> extends DirectiveHandler {

        @NotNull
        private final Class<T> klass;

        CountNodesDirective(@NotNull Class<T> klass) {
            super();
            this.klass = klass;
        }

        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            String functionName = arguments.getNamedArgument("function");
            String countStr = arguments.findNamedArgument("count");
            String maxCountStr = arguments.findNamedArgument("max");
            String includeNestedDeclarations = arguments.findNamedArgument("includeNestedDeclarations");

            JsFunction function = AstSearchUtil.getFunction(ast, functionName);
            List<T> nodes = collectInstances(klass, function.getBody(), includeNestedDeclarations != null && includeNestedDeclarations.equals("true"));
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
            } else if (maxCountStr != null) {
                int expectedCount = Integer.valueOf(maxCountStr);

                String message = "Function " + functionName + " contains " + actualCount +
                                 " nodes of type " + klass.getName() +
                                 ", but expected max is " + expectedCount;
                assertTrue(expectedCount >= actualCount, message);

            } else {
                throw new IllegalArgumentException("'max' or 'count' argument should be provided");
            }
        }

        protected int getActualCountFor(@NotNull T node, @NotNull ArgumentsHelper arguments) {
            return 1;
        }
    }

    private static final DirectiveHandler COUNT_LABELS = new CountNodesDirective<JsLabel>(JsLabel.class) {
        @Override
        protected int getActualCountFor(@NotNull JsLabel node, @NotNull ArgumentsHelper arguments) {
            String labelName = arguments.findNamedArgument("name");
            if (labelName == null) {
                return 1;
            }
            return node.getName().getIdent().equals(labelName) ? 1 : 0;
        }
    };

    private static final DirectiveHandler COUNT_VARS = new CountNodesDirective<>(JsVars.JsVar.class);

    private static final DirectiveHandler COUNT_BREAKS = new CountNodesDirective<>(JsBreak.class);

    private static final DirectiveHandler COUNT_NULLS = new CountNodesDirective<>(JsNullLiteral.class);

    private static final DirectiveHandler COUNT_NEW = new CountNodesDirective<>(JsNew.class);

    private static final DirectiveHandler COUNT_CASES = new CountNodesDirective<>(JsCase.class);

    private static final DirectiveHandler COUNT_IF = new CountNodesDirective<>(JsIf.class);

    private static final DirectiveHandler COUNT_SUPER = new CountNodesDirective<>(JsSuperRef.class);

    private static final DirectiveHandler COUNT_STRING_LITERALS = new CountNodesDirective<>(JsStringLiteral.class);

    private static final DirectiveHandler NOT_REFERENCED = new DirectiveHandler() {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
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

    private static final DirectiveHandler CHECK_COMMENT_EXISTS = new NodeExistenceDirective("CHECK_COMMENT_EXISTS", true) {
        private String text;
        private boolean isMultiLine;

        @Override
        protected String getTextForError() {
            return (isMultiLine ? "Multi line" : "Single line") + " comment with text '" + text + "' doesn't exist";
        }

        @Override
        protected JsVisitor getJsVisitorForElement() {
            return new RecursiveJsVisitor() {
                @Override
                protected void visitElement(@NotNull JsNode node) {
                    checkCommentExistsIn(node.getCommentsBeforeNode());
                    checkCommentExistsIn(node.getCommentsAfterNode());
                    super.visitElement(node);
                }

                @Override
                public void visitSingleLineComment(JsSingleLineComment comment) {
                    checkCommentExistsIn(Arrays.asList(comment));
                }

                @Override
                public void visitMultiLineComment(JsMultiLineComment comment) {
                    checkCommentExistsIn(Arrays.asList(comment));
                }
                private void checkCommentExistsIn(List<JsComment> comments) {
                    if (comments == null) return;
                    for (JsComment comment : comments) {
                        if (isNeededCommentType(comment) && isTheSameText(comment.getText(), text)) {
                            setElementExists(true);
                        }
                    }
                }

                private boolean isNeededCommentType(JsComment comment) {
                    return isMultiLine ? comment instanceof JsMultiLineComment : comment instanceof  JsSingleLineComment;
                }
            };
        }

        @Override
        protected void loadArguments(@NotNull ArgumentsHelper arguments) {
            this.text = arguments.findNamedArgument("text").replace("\\n", System.lineSeparator());;
            this.isMultiLine = Boolean.parseBoolean(arguments.findNamedArgument("multiline"));
        }

        private boolean isTheSameText(String str1, String str2) {
            List<String> lines1 = StringsKt.lines(str1);
            List<String> lines2 = StringsKt.lines(str2);

            if (lines1.size() != lines2.size()) return false;

            for (int i = 0; i < lines1.size(); i++) {
                if (!lines1.get(i).trim().equals(lines2.get(i).trim())) return false;
            }

            return true;
        }

    };

    private static final DirectiveHandler HAS_NO_CAPTURED_VARS = new DirectiveHandler() {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
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
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_CALLED_IN_SCOPE(), FUNCTION_CALLED_IN_SCOPE),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_NOT_CALLED_IN_SCOPE(), FUNCTION_NOT_CALLED_IN_SCOPE),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_COMMENT_EXISTS(), CHECK_COMMENT_EXISTS),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_LABELS_COUNT(), COUNT_LABELS),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_VARS_COUNT(), COUNT_VARS),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_BREAKS_COUNT(), COUNT_BREAKS),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_NULLS_COUNT(), COUNT_NULLS),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_NEW_COUNT(), COUNT_NEW),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_CASES_COUNT(), COUNT_CASES),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_IF_COUNT(), COUNT_IF),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_SUPER_COUNT(), COUNT_SUPER),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_STRING_LITERAL_COUNT(), COUNT_STRING_LITERALS),
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

    public static void checkCalledInScope(
            @NotNull JsNode node,
            @NotNull String functionName,
            @NotNull String scopeFunctionName,
            boolean checkQualifier
    ) throws Exception {
        String errorMessage = functionName + " is not called inside " + scopeFunctionName;
        assertFalse(isCalledInScope(node, functionName, scopeFunctionName, checkQualifier), errorMessage);
    }

    public static void checkNotCalledInScope(
            @NotNull JsNode node,
            @NotNull String functionName,
            @NotNull String scopeFunctionName,
            boolean checkQualifier
    ) throws Exception {
        String errorMessage = functionName + " is called inside " + scopeFunctionName;
        assertTrue(isCalledInScope(node, functionName, scopeFunctionName, checkQualifier), errorMessage);
    }

    private static boolean isCalledInScope(
            @NotNull JsNode node,
            @NotNull String functionName,
            @NotNull String scopeFunctionName,
            boolean checkQualifier
    ) throws Exception {
        JsNode scope = AstSearchUtil.getFunction(node, scopeFunctionName);

        CallCounter counter = CallCounter.countCalls(scope);
        if (checkQualifier) {
            return counter.getQualifiedCallsCount(functionName) == 0;
        }
        else {
            return counter.getUnqualifiedCallsCount(functionName) == 0;
        }
    }

    private static class DirectiveHandler<A extends ArgumentsHelper> {

        /**
         * Processes directive entries.
         *
         * Each entry is expected to have the following format:
         * `// DIRECTIVE: arguments
         *
         * @see ArgumentsHelper for arguments format
         */
        void process(@NotNull JsNode ast,
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

        void processEntry(@NotNull JsNode ast, @NotNull A arguments, File sourceFile) throws Exception {
            if (arguments instanceof JsAstDirective) {
                ((JsAstDirective) arguments).evaluate(ast, sourceFile);
            }
        }
    }
}
