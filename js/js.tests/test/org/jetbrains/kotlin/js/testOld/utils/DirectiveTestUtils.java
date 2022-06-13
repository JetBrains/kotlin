/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.testOld.utils;

import com.intellij.openapi.util.text.StringUtil;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.inline.util.CollectUtilsKt;
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata;
import org.jetbrains.kotlin.test.TargetBackend;
import org.junit.runners.model.MultipleFailureException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.jetbrains.kotlin.js.inline.util.CollectUtilsKt.collectInstances;
import static org.jetbrains.kotlin.test.InTextDirectivesUtils.findLinesWithPrefixesRemoved;
import static org.junit.Assert.*;

public class DirectiveTestUtils {

    private DirectiveTestUtils() {}

    private static final DirectiveHandler FUNCTION_CONTAINS_NO_CALLS = new DirectiveHandler("CHECK_CONTAINS_NO_CALLS") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
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
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkFunctionNotCalled(ast, arguments.getFirst(), arguments.findNamedArgument("except"));
        }
    };

    private static final DirectiveHandler PROPERTY_NOT_USED = new DirectiveHandler("PROPERTY_NOT_USED") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkPropertyNotUsed(ast, arguments.getFirst(), arguments.findNamedArgument("scope"), false, false);
        }
    };

    private static final DirectiveHandler PROPERTY_NOT_READ_FROM = new DirectiveHandler("PROPERTY_NOT_READ_FROM") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkPropertyNotUsed(ast, arguments.getFirst(), arguments.findNamedArgument("scope"), false, true);
        }
    };

    private static final DirectiveHandler PROPERTY_NOT_WRITTEN_TO = new DirectiveHandler("PROPERTY_NOT_WRITTEN_TO") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkPropertyNotUsed(ast, arguments.getFirst(), arguments.findNamedArgument("scope"), true, false);
        }
    };

    private static final DirectiveHandler PROPERTY_WRITE_COUNT = new DirectiveHandler("PROPERTY_WRITE_COUNT") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkPropertyWriteCount(ast, arguments.getNamedArgument("name"), arguments.findNamedArgument("scope"),
                                    Integer.parseInt(arguments.getNamedArgument("count")));
        }
    };

    private static final DirectiveHandler PROPERTY_READ_COUNT = new DirectiveHandler("PROPERTY_READ_COUNT") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkPropertyReadCount(ast, arguments.getNamedArgument("name"), arguments.findNamedArgument("scope"),
                                   Integer.parseInt(arguments.getNamedArgument("count")));
        }
    };

    private static final DirectiveHandler FUNCTION_EXISTS = new DirectiveHandler("CHECK_FUNCTION_EXISTS") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            AstSearchUtil.getFunction(ast, arguments.getFirst());
        }
    };

    private static final DirectiveHandler FUNCTION_CALLED_IN_SCOPE = new DirectiveHandler("CHECK_CALLED_IN_SCOPE") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            // Be more restrictive, check qualified match by default
            checkCalledInScope(ast, arguments.getNamedArgument("function"), arguments.getNamedArgument("scope"),
                               parseBooleanArgument(arguments, "qualified", true));
        }
    };

    private static final DirectiveHandler FUNCTION_NOT_CALLED_IN_SCOPE = new DirectiveHandler("CHECK_NOT_CALLED_IN_SCOPE") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            // Be more restrictive, check unqualified match by default
            checkNotCalledInScope(ast, arguments.getNamedArgument("function"), arguments.getNamedArgument("scope"),
                                  parseBooleanArgument(arguments, "qualified", false));
        }
    };

    private static final DirectiveHandler FUNCTION_CALLED_TIMES = new DirectiveHandler("FUNCTION_CALLED_TIMES") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            int expectedCount = Integer.parseInt(arguments.getNamedArgument("count"));
            String functionName = arguments.getFirst();
            CallCounter counter = CallCounter.countCalls(ast);
            int actualCount = counter.getUnqualifiedCallsCount(functionName);
            assertEquals("Function " + functionName, expectedCount, actualCount);
        }
    };

    private static boolean parseBooleanArgument(@NotNull ArgumentsHelper arguments, @NotNull String name, boolean defaultValue) {
        String value = arguments.findNamedArgument(name);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    private static final DirectiveHandler FUNCTIONS_HAVE_SAME_LINES = new DirectiveHandler("CHECK_FUNCTIONS_HAVE_SAME_LINES") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            String code1 = getFunctionCode(ast, arguments.getPositionalArgument(0));
            String code2 = getFunctionCode(ast, arguments.getPositionalArgument(1));

            String regexMatch = arguments.findNamedArgument("match");
            String regexReplace = arguments.findNamedArgument("replace");

            code1 = applyRegex(code1, regexMatch, regexReplace);
            code2 = applyRegex(code2, regexMatch, regexReplace);

            assertEquals(code1, code2);
        }

        @NotNull
        String getFunctionCode(@NotNull JsNode ast, @NotNull String functionName) {
            JsFunction function = AstSearchUtil.getFunction(ast, functionName);
            return function.getBody().toString();
        }

        @NotNull
        String applyRegex(@NotNull String code, @Nullable String match, @Nullable String replace) {
            if (match == null || replace == null) return code;

            return code.replaceAll(match, replace);
        }
    };

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
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            loadArguments(arguments);
            getJsVisitorForElement().accept(ast);
            assertExistence();
            setElementExists(false);
        }

        private void assertExistence() {
            String message = getTextForError();
            if (shouldCheckForExistence) {
                assertTrue(message, isElementExists);
            } else {
                assertFalse(message, isElementExists);
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
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            String functionName = arguments.getNamedArgument("function");
            String countStr = arguments.getNamedArgument("count");
            int expectedCount = Integer.valueOf(countStr);

            JsFunction function = AstSearchUtil.getFunction(ast, functionName);
            List<T> nodes = collectInstances(klass, function.getBody());
            int actualCount = 0;

            for (T node : nodes) {
                actualCount += getActualCountFor(node, arguments);
            }

            String message = "Function " + functionName + " contains " + actualCount +
                             " nodes of type " + klass.getName() +
                             ", but expected count is " + expectedCount;
            assertEquals(message, expectedCount, actualCount);
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

    private static final DirectiveHandler COUNT_TERNARY_OPERATOR =
            new CountNodesDirective<>("CHECK_TERNARY_OPERATOR_COUNT", JsConditional.class);

    private static final DirectiveHandler COUNT_BINOPS = new CountNodesDirective<JsBinaryOperation>("CHECK_BINOP_COUNT",
                                                                                                        JsBinaryOperation.class) {
        @Override
        protected int getActualCountFor(@NotNull JsBinaryOperation node, @NotNull ArgumentsHelper arguments) {
            String symbol = arguments.findNamedArgument("symbol");
            if (symbol == null) {
                return 1;
            }
            return node.getOperator().getSymbol().equals(symbol) ? 1 : 0;
        }
    };

    private static final DirectiveHandler COUNT_DEBUGGER = new CountNodesDirective<>("CHECK_DEBUGGER_COUNT", JsDebugger.class);

    private static final DirectiveHandler COUNT_STRING_LITERALS = new CountNodesDirective<>("CHECK_STRING_LITERAL_COUNT", JsStringLiteral.class);

    private static final DirectiveHandler NOT_REFERENCED = new DirectiveHandler("CHECK_NOT_REFERENCED") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
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

    private static final DirectiveHandler ONLY_THIS_QUALIFIED_REFERENCES = new DirectiveHandler("ONLY_THIS_QUALIFIED_REFERENCES") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            String fieldName = arguments.getPositionalArgument(0);
            QualifiedReferenceCollector collector = new QualifiedReferenceCollector(fieldName);
            ast.accept(collector);
            assertTrue("No reference to field '" + fieldName + "' found", collector.hasReferences);
            assertTrue("There are references to field '" + fieldName + "' not qualified by 'this' literal",
                       collector.allReferencesQualifiedByThis);
        }
    };

    static class QualifiedReferenceCollector extends RecursiveJsVisitor {
        private final String nameToSearch;
        boolean hasReferences;
        boolean allReferencesQualifiedByThis = true;

        public QualifiedReferenceCollector(String nameToSearch) {
            this.nameToSearch = nameToSearch;
        }

        @Override
        public void visitNameRef(@NotNull JsNameRef nameRef) {
            super.visitNameRef(nameRef);
            JsName name = nameRef.getName();
            if (name == null) return;

            if (name.getIdent().equals(nameToSearch)) {
                hasReferences = true;
                if (!(nameRef.getQualifier() instanceof JsThisRef)) {
                    allReferencesQualifiedByThis = false;
                }
            }
        }
    }

    private static final DirectiveHandler HAS_INLINE_METADATA = new DirectiveHandler("CHECK_HAS_INLINE_METADATA") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            String functionName = arguments.getPositionalArgument(0);
            JsExpression property = AstSearchUtil.getMetadataOrFunction(ast, functionName);
            String message = "Inline metadata has not been generated for function " + functionName;
            assertNotNull(message, InlineMetadata.decompose(property));
        }
    };

    private static final DirectiveHandler HAS_NO_INLINE_METADATA = new DirectiveHandler("CHECK_HAS_NO_INLINE_METADATA") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            String functionName = arguments.getPositionalArgument(0);
            JsExpression property = AstSearchUtil.getMetadataOrFunction(ast, functionName);
            String message = "Inline metadata has been generated for not effectively public function " + functionName;
            assertTrue(message, property instanceof JsFunction);
        }
    };

    private static final DirectiveHandler HAS_NO_CAPTURED_VARS = new DirectiveHandler("HAS_NO_CAPTURED_VARS") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
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
                assertTrue("Function " + functionName + " captures free variable " + freeVar.getIdent(),
                           except.contains(freeVar.getIdent()));
            }
        }
    };

    private static final DirectiveHandler DECLARES_VARIABLE = new DirectiveHandler("DECLARES_VARIABLE") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            String functionName = arguments.getNamedArgument("function");
            String varName = arguments.getNamedArgument("name");
            List<JsFunction> functions = AstSearchUtil.getFunctions(ast, functionName);
            boolean[] varDeclared = new boolean[1];
            for (JsFunction function : functions) {
                function.accept(new RecursiveJsVisitor() {
                    @Override
                    public void visit(@NotNull JsVars.JsVar x) {
                        super.visit(x);
                        if (x.getName().getIdent().equals(varName)) {
                            varDeclared[0] = true;
                        }
                    }
                });
                if (varDeclared[0])
                    break;
            }

            assertTrue("Function " + functionName + " does not declare variable " + varName, varDeclared[0]);
        }
    };

    private static final List<DirectiveHandler> DIRECTIVE_HANDLERS = Arrays.asList(
            FUNCTION_CONTAINS_NO_CALLS,
            FUNCTION_NOT_CALLED,
            FUNCTION_CALLED_TIMES,
            PROPERTY_NOT_USED,
            PROPERTY_NOT_READ_FROM,
            PROPERTY_NOT_WRITTEN_TO,
            PROPERTY_READ_COUNT,
            PROPERTY_WRITE_COUNT,
            FUNCTION_EXISTS,
            FUNCTION_CALLED_IN_SCOPE,
            FUNCTION_NOT_CALLED_IN_SCOPE,
            FUNCTIONS_HAVE_SAME_LINES,
            ONLY_THIS_QUALIFIED_REFERENCES,
            CHECK_COMMENT_EXISTS,
            COUNT_LABELS,
            COUNT_VARS,
            COUNT_BREAKS,
            COUNT_NULLS,
            COUNT_NEW,
            COUNT_CASES,
            COUNT_IF,
            COUNT_TERNARY_OPERATOR,
            COUNT_BINOPS,
            COUNT_DEBUGGER,
            COUNT_STRING_LITERALS,
            NOT_REFERENCED,
            HAS_INLINE_METADATA,
            HAS_NO_INLINE_METADATA,
            HAS_NO_CAPTURED_VARS,
            DECLARES_VARIABLE
    );

    public static void processDirectives(
            @NotNull JsNode ast,
            @NotNull String sourceCode,
            @NotNull TargetBackend targetBackend
    ) throws Exception {
        List<Throwable> assertionErrors = new ArrayList<>();
        for (DirectiveHandler handler : DIRECTIVE_HANDLERS) {
            handler.process(ast, sourceCode, targetBackend, assertionErrors);
        }
        MultipleFailureException.assertEmpty(assertionErrors);
    }

    public static void checkFunctionContainsNoCalls(JsNode node, String functionName, @NotNull Set<String> exceptFunctionNames)
            throws Exception {
        JsFunction function = AstSearchUtil.getFunction(node, functionName);
        CallCounter counter = CallCounter.countCalls(function, exceptFunctionNames);
        int callsCount = counter.getTotalCallsCount();

        String errorMessage = functionName + " contains calls";
        assertEquals(errorMessage, 0, callsCount);
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
            assertFalse("property getter for `" + propertyName + "`"  + " in scope: " + scope + " is called",
                        counter.hasUnqualifiedReads(propertyName));
        }
        if (!isSetAllowed) {
            assertFalse("property setter for `" + propertyName + "`"  + " in scope: " + scope + " is called",
                        counter.hasUnqualifiedWrites(propertyName));
        }
    }

    private static void checkPropertyReadCount(JsNode node, String propertyName, String scope, int expectedCount) throws Exception {
        PropertyReferenceCollector counter = PropertyReferenceCollector.Companion.collect(findScope(node, scope));
        assertEquals("Property read count: " + propertyName + " in scope: " + scope,
                     expectedCount, counter.unqualifiedReadCount(propertyName));
    }

    private static void checkPropertyWriteCount(JsNode node, String propertyName, String scope, int expectedCount) throws Exception {
        PropertyReferenceCollector counter = PropertyReferenceCollector.Companion.collect(findScope(node, scope));
        assertEquals("Property write count: " + propertyName + " in scope: " + scope,
                     expectedCount, counter.unqualifiedWriteCount(propertyName));
    }

    public static void checkFunctionNotCalled(@NotNull JsNode node, @NotNull String functionName, @Nullable String exceptFunction)
            throws Exception {
        Set<String> excludedScopes = exceptFunction != null ? Collections.singleton(exceptFunction) : Collections.emptySet();

        CallCounter counter = CallCounter.countCallsWithExcludedScopes(node, excludedScopes);
        int functionCalledCount = counter.getQualifiedCallsCount(functionName);

        String errorMessage = "inline function `" + functionName + "` is called";
        assertEquals(errorMessage, 0, functionCalledCount);
        assertEquals("Not all excluded scopes found", excludedScopes.size(), counter.getExcludedScopeOccurrenceCount());
    }

    public static void checkCalledInScope(
            @NotNull JsNode node,
            @NotNull String functionName,
            @NotNull String scopeFunctionName,
            boolean checkQualifier
    ) throws Exception {
        String errorMessage = functionName + " is not called inside " + scopeFunctionName;
        assertFalse(errorMessage, isCalledInScope(node, functionName, scopeFunctionName, checkQualifier));
    }

    public static void checkNotCalledInScope(
            @NotNull JsNode node,
            @NotNull String functionName,
            @NotNull String scopeFunctionName,
            boolean checkQualifier
    ) throws Exception {
        String errorMessage = functionName + " is called inside " + scopeFunctionName;
        assertTrue(errorMessage, isCalledInScope(node, functionName, scopeFunctionName, checkQualifier));
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

        private final static String TARGET_BACKENDS = "TARGET_BACKENDS";

        private final static String IGNORED_BACKENDS = "IGNORED_BACKENDS";

        @NotNull private final String directive;

        DirectiveHandler(@NotNull String directive) {
            this.directive = "// " + directive + ": ";
        }

        private static boolean containsBackend(
                @NotNull TargetBackend targetBackend,
                @NotNull String backendsParameterName,
                @NotNull ArgumentsHelper arguments,
                boolean ifNotSpecified
        ) {
            String backendsArg = arguments.findNamedArgument(backendsParameterName);
            if (backendsArg != null) {
                List<String> backends = Arrays.asList(backendsArg.split(";"));
                return backends.contains(targetBackend.name());
            }
            return ifNotSpecified;
        }

        /**
         * Processes directive entries.
         *
         * Each entry is expected to have the following format:
         * `// DIRECTIVE: arguments
         *
         * @see ArgumentsHelper for arguments format
         */
        void process(@NotNull JsNode ast, @NotNull String sourceCode,
                @NotNull TargetBackend targetBackend,
                List<Throwable> assertionErrors
        ) throws Exception {
            List<String> directiveEntries = findLinesWithPrefixesRemoved(sourceCode, directive);
            for (String directiveEntry : directiveEntries) {
                ArgumentsHelper arguments = new ArgumentsHelper(directiveEntry);
                if (!containsBackend(targetBackend, TARGET_BACKENDS, arguments, true) ||
                    containsBackend(targetBackend, IGNORED_BACKENDS, arguments, false)) {
                    continue;
                }
                try {
                    processEntry(ast, arguments);
                } catch (AssertionError e) {
                    assertionErrors.add(e);
                }
            }
        }

        abstract void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception;

        @Override
        public String toString() {
            return getName();
        }

        @NotNull
        String getName() {
            return directive;
        }
    }

    /**
     * Arguments format: ((namedArg|positionalArg)\s+)*`
     *
     * Where: namedArg -- 'key=value' or 'key="spaced value"'
     *        positionalArg -- 'value'
     *
     * Neither key, nor value should contain spaces.
     */
    private static class ArgumentsHelper {
        private final List<String> positionalArguments = new ArrayList<>();
        private final Map<String, String> namedArguments = new HashMap<>();
        private final String entry;
        private final Pattern argumentsPattern = Pattern.compile("[\\w$_;\\.]+(=((\".*?\")|[\\w$_;\\.]+))?");

        ArgumentsHelper(@NotNull String directiveEntry) {
            entry = directiveEntry;

            Matcher matcher = argumentsPattern.matcher(directiveEntry);

            while (matcher.find()) {
                String argument = matcher.group();
                String[] keyVal = argument.split("=");
                switch (keyVal.length) {
                    case 1: positionalArguments.add(keyVal[0]); break;
                    case 2:
                        String value = keyVal[1];
                        if (value.charAt(0) == '"') {
                            value = value.substring(1, value.length() - 1);
                        }
                        namedArguments.put(keyVal[0], value);
                        break;
                    default: throw new AssertionError("Wrong argument format: " + argument);
                }
            }
        }

        @NotNull
        String getFirst() {
            return getPositionalArgument(0);
        }

        @NotNull
        String getPositionalArgument(int index) {
            assert positionalArguments.size() > index: "Argument at index `" + index + "` not found in entry: " + entry;
            return positionalArguments.get(index);
        }

        @NotNull
        String getNamedArgument(@NotNull String name) {
            assert namedArguments.containsKey(name): "Argument `" + name + "` not found in entry: " + entry;
            return namedArguments.get(name);
        }

        @Nullable
        String findNamedArgument(@NotNull String name) {
            return namedArguments.get(name);
        }
    }
}
