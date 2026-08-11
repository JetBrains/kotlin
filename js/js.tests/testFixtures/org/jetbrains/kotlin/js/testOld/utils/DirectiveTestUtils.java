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
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestDataAssertions;
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives;
import org.jetbrains.kotlin.test.directives.model.ValueDirective;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.util.*;

import static kotlin.test.AssertionsKt.assertFalse;
import static kotlin.test.AssertionsKt.assertTrue;
import static org.jetbrains.kotlin.js.inline.util.CollectUtilsKt.collectInstances;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DirectiveTestUtils {

    private DirectiveTestUtils() {}

    private static final DirectiveHandler FUNCTION_CONTAINS_NO_CALLS = new DirectiveHandler("CHECK_CONTAINS_NO_CALLS") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            Set<String> exceptNames = new HashSet<>();
            String exceptNamesArg = arguments.findNamedArgument("except");
            if (exceptNamesArg != null) {
                for (String exceptName : exceptNamesArg.split(";")) {
                    exceptNames.add(exceptName.trim());
                }
            }

            checkFunctionContainsNoCalls(ast, arguments.getFirst(), exceptNames);
        }
    };

    private static final DirectiveHandler FUNCTION_NOT_CALLED = new DirectiveHandler("CHECK_NOT_CALLED") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            checkFunctionNotCalled(ast, arguments.getFirst(), arguments.findNamedArgument("except"));
        }
    };

    private static final DirectiveHandler PROPERTY_NOT_USED = new DirectiveHandler("PROPERTY_NOT_USED") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            checkPropertyNotUsed(ast, arguments.getFirst(), arguments.findNamedArgument("scope"), false, false);
        }
    };

    private static final DirectiveHandler PROPERTY_NOT_READ_FROM = new DirectiveHandler("PROPERTY_NOT_READ_FROM") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            checkPropertyNotUsed(ast, arguments.getFirst(), arguments.findNamedArgument("scope"), false, true);
        }
    };

    private static final DirectiveHandler PROPERTY_NOT_WRITTEN_TO = new DirectiveHandler("PROPERTY_NOT_WRITTEN_TO") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            checkPropertyNotUsed(ast, arguments.getFirst(), arguments.findNamedArgument("scope"), true, false);
        }
    };

    private static final DirectiveHandler PROPERTY_WRITE_COUNT = new DirectiveHandler("PROPERTY_WRITE_COUNT") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            checkPropertyWriteCount(ast, arguments.getNamedArgument("name"), arguments.findNamedArgument("scope"),
                                    Integer.parseInt(arguments.getNamedArgument("count")));
        }
    };

    private static final DirectiveHandler PROPERTY_READ_COUNT = new DirectiveHandler("PROPERTY_READ_COUNT") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            checkPropertyReadCount(ast, arguments.getNamedArgument("name"), arguments.findNamedArgument("scope"),
                                   Integer.parseInt(arguments.getNamedArgument("count")));
        }
    };

    private static final DirectiveHandler EXPECT_GENERATED_JS = new DirectiveHandler("EXPECT_GENERATED_JS") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) {
            List<String> functionNames = arguments.findNamedListArgument("function");
            List<String> classesNames = arguments.findNamedListArgument("class");
            String expected = arguments.getNamedArgument("expect");
            File expectedFile = new File(sourceFile.getParentFile(), expected);
            StringBuilder code = new StringBuilder();
            for (String functionName : functionNames) {
                code.append(AstSearchUtil.getFunction(ast, functionName));
                code.append("\n");
            }
            for (String className : classesNames) {
                code.append(AstSearchUtil.getClass(ast, className));
                code.append("\n");
            }
            String msg = "Functions " + StringUtil.join(functionNames, ", ") + " or classes " + StringUtil.join(classesNames, ", ") + " got different generated JS code";
            TestDataAssertions.assertEqualsToFile(msg, expectedFile, code.toString());
        }
    };

    private static final DirectiveHandler CLASS_EXISTS = new DirectiveHandler("CHECK_CLASS_EXISTS") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) {
            AstSearchUtil.getClass(ast, arguments.getFirst());
        }
    };

    private static final DirectiveHandler FUNCTION_EXISTS = new DirectiveHandler("CHECK_FUNCTION_EXISTS") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            AstSearchUtil.getFunction(ast, arguments.getFirst());
        }
    };

    private static final DirectiveHandler FUNCTION_CALLED_IN_SCOPE = new DirectiveHandler("CHECK_CALLED_IN_SCOPE") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            // Be more restrictive, check qualified match by default
            checkCalledInScope(ast, arguments.getNamedArgument("function"), arguments.getNamedArgument("scope"),
                               parseBooleanArgument(arguments, "qualified", true));
        }
    };

    private static final DirectiveHandler FUNCTION_NOT_CALLED_IN_SCOPE = new DirectiveHandler("CHECK_NOT_CALLED_IN_SCOPE") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            // Be more restrictive, check unqualified match by default
            checkNotCalledInScope(ast, arguments.getNamedArgument("function"), arguments.getNamedArgument("scope"),
                                  parseBooleanArgument(arguments, "qualified", false));
        }
    };

    private static final DirectiveHandler FUNCTION_CALLED_TIMES = new DirectiveHandler("FUNCTION_CALLED_TIMES") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception {
            int expectedCount = Integer.parseInt(arguments.getNamedArgument("count"));
            String functionName = arguments.getFirst();
            CallCounter counter = CallCounter.countCalls(ast);
            int actualCount = counter.getUnqualifiedCallsCount(functionName);
            assertEquals(expectedCount, actualCount, "Function " + functionName);
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
            super(directive);
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

        CountNodesDirective(@NotNull String directive, @NotNull Class<T> klass) {
            super(directive);
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

    private static final DirectiveHandler COUNT_LABELS = new CountNodesDirective<JsLabel>("CHECK_LABELS_COUNT", JsLabel.class) {
        @Override
        protected int getActualCountFor(@NotNull JsLabel node, @NotNull ArgumentsHelper arguments) {
            String labelName = arguments.findNamedArgument("name");
            if (labelName == null) {
                return 1;
            }
            return node.getName().getIdent().equals(labelName) ? 1 : 0;
        }
    };

    private static final DirectiveHandler COUNT_VARS = new CountNodesDirective<>("CHECK_VARS_COUNT", JsVars.JsVar.class);

    private static final DirectiveHandler COUNT_BREAKS = new CountNodesDirective<>("CHECK_BREAKS_COUNT", JsBreak.class);

    private static final DirectiveHandler COUNT_NULLS = new CountNodesDirective<>("CHECK_NULLS_COUNT", JsNullLiteral.class);

    private static final DirectiveHandler COUNT_NEW = new CountNodesDirective<>("CHECK_NEW_COUNT", JsNew.class);

    private static final DirectiveHandler COUNT_CASES = new CountNodesDirective<>("CHECK_CASES_COUNT", JsCase.class);

    private static final DirectiveHandler COUNT_IF = new CountNodesDirective<>("CHECK_IF_COUNT", JsIf.class);

    private static final DirectiveHandler COUNT_SUPER = new CountNodesDirective<>("CHECK_SUPER_COUNT", JsSuperRef.class);

    private static final DirectiveHandler COUNT_STRING_LITERALS = new CountNodesDirective<>("CHECK_STRING_LITERAL_COUNT", JsStringLiteral.class);

    private static final DirectiveHandler NOT_REFERENCED = new DirectiveHandler("CHECK_NOT_REFERENCED") {
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

    private static final DirectiveHandler HAS_NO_CAPTURED_VARS = new DirectiveHandler("HAS_NO_CAPTURED_VARS") {
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

    private static final List<Pair<ValueDirective<ArgumentsHelper>, DirectiveHandler>> DIRECTIVE_HANDLERS = Arrays.asList(
            new Pair<>(JsAstDirectives.INSTANCE.getEXPECT_GENERATED_JS(), EXPECT_GENERATED_JS),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_CONTAINS_NO_CALLS(), FUNCTION_CONTAINS_NO_CALLS),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_NOT_CALLED(), FUNCTION_NOT_CALLED),
            new Pair<>(JsAstDirectives.INSTANCE.getFUNCTION_CALLED_TIMES(), FUNCTION_CALLED_TIMES),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_NOT_USED(), PROPERTY_NOT_USED),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_NOT_READ_FROM(), PROPERTY_NOT_READ_FROM),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_NOT_WRITTEN_TO(), PROPERTY_NOT_WRITTEN_TO),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_READ_COUNT(), PROPERTY_READ_COUNT),
            new Pair<>(JsAstDirectives.INSTANCE.getPROPERTY_WRITE_COUNT(), PROPERTY_WRITE_COUNT),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_CLASS_EXISTS(), CLASS_EXISTS),
            new Pair<>(JsAstDirectives.INSTANCE.getCHECK_FUNCTION_EXISTS(), FUNCTION_EXISTS),
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

    public static void processDirectives(
            @NotNull JsNode ast,
            @NotNull File sourceFile,
            @NotNull TargetBackend targetBackend,
            @NotNull RegisteredDirectives allDirectives
    ) {
        Assertions.assertAll(DIRECTIVE_HANDLERS.stream().map((directiveAndHandler -> {
            List<ArgumentsHelper> directiveEntries=allDirectives.get(directiveAndHandler.getFirst());
            return () -> directiveAndHandler.getSecond().process(ast, sourceFile, directiveEntries, targetBackend);
        })));
    }

    public static void checkFunctionContainsNoCalls(JsNode node, String functionName, @NotNull Set<String> exceptFunctionNames)
            throws Exception {
        JsFunction function = AstSearchUtil.getFunction(node, functionName);
        CallCounter counter = CallCounter.countCalls(function, exceptFunctionNames);
        int callsCount = counter.getTotalCallsCount();

        String errorMessage = functionName + " contains calls";
        assertEquals(0, callsCount, errorMessage);
    }

    @NotNull
    public static JsNode findScope(@NotNull JsNode node, @Nullable String scopeFunctionName) {
        if (scopeFunctionName != null) {
            return AstSearchUtil.getFunction(node, scopeFunctionName);
        }
        return node;
    }

    public static void checkPropertyNotUsed(JsNode node, String propertyName, String scope, boolean isGetAllowed, boolean isSetAllowed)
            throws Exception {
        PropertyReferenceCollector counter = PropertyReferenceCollector.Companion.collect(findScope(node, scope));
        if (!isGetAllowed) {
            assertFalse(counter.hasUnqualifiedReads(propertyName),
                        "property getter for `" + propertyName + "`"  + " in scope: " + scope + " is called");
        }
        if (!isSetAllowed) {
            assertFalse(counter.hasUnqualifiedWrites(propertyName),
                        "property setter for `" + propertyName + "`"  + " in scope: " + scope + " is called");
        }
    }

    private static void checkPropertyReadCount(JsNode node, String propertyName, String scope, int expectedCount) throws Exception {
        PropertyReferenceCollector counter = PropertyReferenceCollector.Companion.collect(findScope(node, scope));
        assertEquals(expectedCount, counter.unqualifiedReadCount(propertyName),
                     "Property read count: " + propertyName + " in scope: " + scope);
    }

    private static void checkPropertyWriteCount(JsNode node, String propertyName, String scope, int expectedCount) throws Exception {
        PropertyReferenceCollector counter = PropertyReferenceCollector.Companion.collect(findScope(node, scope));
        assertEquals(expectedCount, counter.unqualifiedWriteCount(propertyName),
                     "Property write count: " + propertyName + " in scope: " + scope
        );
    }

    public static void checkFunctionNotCalled(@NotNull JsNode node, @NotNull String functionName, @Nullable String exceptFunction)
            throws Exception {
        Set<String> excludedScopes = exceptFunction != null ? Collections.singleton(exceptFunction) : Collections.emptySet();

        CallCounter counter = CallCounter.countCallsWithExcludedScopes(node, excludedScopes);
        int functionCalledCount = counter.getQualifiedCallsCount(functionName);

        String errorMessage = "inline function `" + functionName + "` is called";
        assertEquals(0, functionCalledCount, errorMessage);
        assertEquals(excludedScopes.size(), counter.getExcludedScopeOccurrenceCount(), "Not all excluded scopes found");
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

    private abstract static class DirectiveHandler {

        @NotNull private final String directive;

        DirectiveHandler(@NotNull String directive) {
            this.directive = "// " + directive + ": ";
        }

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
                @NotNull List<ArgumentsHelper> directiveEntries,
                @NotNull TargetBackend targetBackend
        ) throws Exception {
            for (ArgumentsHelper arguments : directiveEntries) {
                if (arguments.shouldRunWithBackend(targetBackend)) {
                    processEntry(ast, arguments, sourceFile);
                }
            }
        }

        abstract void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments, File sourceFile) throws Exception;

        @Override
        public String toString() {
            return getName();
        }

        @NotNull
        String getName() {
            return directive;
        }
    }
}
