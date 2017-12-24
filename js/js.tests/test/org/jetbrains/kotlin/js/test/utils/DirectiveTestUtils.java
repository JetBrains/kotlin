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

package org.jetbrains.kotlin.js.test.utils;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.inline.util.CollectUtilsKt;
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata;

import java.util.*;

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
            checkPropertyNotUsed(ast, arguments.getFirst(), false, false);
        }
    };

    private static final DirectiveHandler PROPERTY_NOT_READ_FROM = new DirectiveHandler("PROPERTY_NOT_READ_FROM") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkPropertyNotUsed(ast, arguments.getFirst(), false, true);
        }
    };

    private static final DirectiveHandler PROPERTY_NOT_WRITTEN_TO = new DirectiveHandler("PROPERTY_NOT_WRITTEN_TO") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkPropertyNotUsed(ast, arguments.getFirst(), true, false);
        }
    };

    private static final DirectiveHandler PROPERTY_WRITE_COUNT = new DirectiveHandler("PROPERTY_WRITE_COUNT") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkPropertyWriteCount(ast, arguments.getNamedArgument("name"), Integer.parseInt(arguments.getNamedArgument("count")));
        }
    };

    private static final DirectiveHandler PROPERTY_READ_COUNT = new DirectiveHandler("PROPERTY_READ_COUNT") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkPropertyReadCount(ast, arguments.getNamedArgument("name"), Integer.parseInt(arguments.getNamedArgument("count")));
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

    private static final DirectiveHandler COUNT_DEBUGGER = new CountNodesDirective<>("CHECK_DEBUGGER_COUNT", JsDebugger.class);

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
            JsFunction function = AstSearchUtil.getFunction(ast, functionName);
            boolean[] varDeclared = new boolean[1];
            function.accept(new RecursiveJsVisitor() {
                @Override
                public void visit(@NotNull JsVars.JsVar x) {
                    super.visit(x);
                    if (x.getName().getIdent().equals(varName)) {
                        varDeclared[0] = true;
                    }
                }
            });

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
            FUNCTION_CALLED_IN_SCOPE,
            FUNCTION_NOT_CALLED_IN_SCOPE,
            FUNCTIONS_HAVE_SAME_LINES,
            ONLY_THIS_QUALIFIED_REFERENCES,
            COUNT_LABELS,
            COUNT_VARS,
            COUNT_BREAKS,
            COUNT_NULLS,
            COUNT_DEBUGGER,
            NOT_REFERENCED,
            HAS_INLINE_METADATA,
            HAS_NO_INLINE_METADATA,
            HAS_NO_CAPTURED_VARS,
            DECLARES_VARIABLE
    );

    public static void processDirectives(@NotNull JsNode ast, @NotNull String sourceCode) throws Exception {
        for (DirectiveHandler handler : DIRECTIVE_HANDLERS) {
            handler.process(ast, sourceCode);
        }
    }

    public static void checkFunctionContainsNoCalls(JsNode node, String functionName, @NotNull Set<String> exceptFunctionNames)
            throws Exception {
        JsFunction function = AstSearchUtil.getFunction(node, functionName);
        CallCounter counter = CallCounter.countCalls(function, exceptFunctionNames);
        int callsCount = counter.getTotalCallsCount();

        String errorMessage = functionName + " contains calls";
        assertEquals(errorMessage, 0, callsCount);
    }

    public static void checkPropertyNotUsed(JsNode node, String propertyName, boolean isGetAllowed, boolean isSetAllowed) throws Exception {
        PropertyReferenceCollector counter = PropertyReferenceCollector.Companion.collect(node);
        if (!isGetAllowed) {
            assertFalse("inline property getter for `" + propertyName + "` is called", counter.hasUnqualifiedReads(propertyName));
        }
        if (!isSetAllowed) {
            assertFalse("inline property setter for `" + propertyName + "` is called", counter.hasUnqualifiedWrites(propertyName));
        }
    }

    private static void checkPropertyReadCount(JsNode node, String propertyName, int expectedCount) throws Exception {
        PropertyReferenceCollector counter = PropertyReferenceCollector.Companion.collect(node);
        assertEquals("Property read count: " + propertyName, expectedCount, counter.unqualifiedReadCount(propertyName));
    }

    private static void checkPropertyWriteCount(JsNode node, String propertyName, int expectedCount) throws Exception {
        PropertyReferenceCollector counter = PropertyReferenceCollector.Companion.collect(node);
        assertEquals("Property write count: " + propertyName, expectedCount, counter.unqualifiedWriteCount(propertyName));
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
        void process(@NotNull JsNode ast, @NotNull String sourceCode) throws Exception {
            List<String> directiveEntries = findLinesWithPrefixesRemoved(sourceCode, directive);
            for (String directiveEntry : directiveEntries) {
                processEntry(ast, new ArgumentsHelper(directiveEntry));
            }
        }

        abstract void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception;

        @NotNull
        String getName() {
            return directive;
        }
    }

    /**
     * Arguments format: ((namedArg|positionalArg)\s+)*`
     *
     * Where: namedArg -- "key=value"
     *        positionalArg -- "value"
     *
     * Neither key, nor value should contain spaces.
     */
    private static class ArgumentsHelper {
        private final List<String> positionalArguments = new ArrayList<>();
        private final Map<String, String> namedArguments = new HashMap<>();
        private final String entry;

        ArgumentsHelper(@NotNull String directiveEntry) {
            entry = directiveEntry;

            for (String argument: directiveEntry.split("\\s+")) {
                String[] keyVal = argument.split("=");

                switch (keyVal.length) {
                    case 1: positionalArguments.add(keyVal[0]); break;
                    case 2: namedArguments.put(keyVal[0], keyVal[1]); break;
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
