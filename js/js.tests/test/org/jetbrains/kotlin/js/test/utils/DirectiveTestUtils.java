/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.js.inline.util.UtilPackage.collectInstances;
import static org.jetbrains.kotlin.test.InTextDirectivesUtils.findLinesWithPrefixesRemoved;
import static org.junit.Assert.*;

public class DirectiveTestUtils {

    private DirectiveTestUtils() {}

    private static final DirectiveHandler FUNCTION_CONTAINS_NO_CALLS = new DirectiveHandler("CHECK_CONTAINS_NO_CALLS") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkFunctionContainsNoCalls(ast, arguments.getFirst());
        }
    };

    private static final DirectiveHandler FUNCTION_NOT_CALLED = new DirectiveHandler("CHECK_NOT_CALLED") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkFunctionNotCalled(ast, arguments.getFirst());
        }
    };

    private static final DirectiveHandler FUNCTION_CALLED_IN_SCOPE = new DirectiveHandler("CHECK_CALLED_IN_SCOPE") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkCalledInScope(ast, arguments.getNamedArgument("function"), arguments.getNamedArgument("scope"));
        }
    };

    private static final DirectiveHandler FUNCTION_NOT_CALLED_IN_SCOPE = new DirectiveHandler("CHECK_NOT_CALLED_IN_SCOPE") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            checkNotCalledInScope(ast, arguments.getNamedArgument("function"), arguments.getNamedArgument("scope"));
        }
    };

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
            return function.toString();
        }

        @NotNull
        String applyRegex(@NotNull String code, @Nullable String match, @Nullable String replace) {
            if (match == null || replace == null) return code;

            return code.replaceAll(match, replace);
        }
    };

    private abstract static class CountNodesDirective<T extends JsNode> extends DirectiveHandler {

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

            JsNode scope = AstSearchUtil.getFunction(ast, functionName);
            List<T> nodes = collectInstances(klass, scope);
            int actualCount = 0;

            for (T node : nodes) {
                actualCount += getActualCountFor(node, arguments);
            }

            String message = "Function " + functionName + " contains " + actualCount +
                             " nodes of type " + klass.getName() +
                             ", but expected count is " + expectedCount;
            assertEquals(message, expectedCount, actualCount);
        }

        protected abstract int getActualCountFor(@NotNull T node, @NotNull ArgumentsHelper arguments);
    }

    private static final DirectiveHandler COUNT_LABELS = new CountNodesDirective<JsLabel>("CHECK_LABELS_COUNT", JsLabel.class) {
        @Override
        protected int getActualCountFor(@NotNull JsLabel node, @NotNull ArgumentsHelper arguments) {
            String labelName = arguments.getNamedArgument("name");

            if (node.getName().getIdent().equals(labelName)) {
                return 1;
            }

            return 0;
        }
    };

    private static final DirectiveHandler COUNT_VARS = new CountNodesDirective<JsVars>("CHECK_VARS_COUNT", JsVars.class) {
        @Override
        protected int getActualCountFor(@NotNull JsVars node, @NotNull ArgumentsHelper arguments) {
            return node.getVars().size();
        }
    };

    private static final DirectiveHandler HAS_INLINE_METADATA = new DirectiveHandler("CHECK_HAS_INLINE_METADATA") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            String functionName = arguments.getPositionalArgument(0);
            JsExpression property = AstSearchUtil.getProperty(ast, functionName);
            String message = "Inline metadata has not been generated for function " + functionName;
            assertNotNull(message, InlineMetadata.decompose(property));
        }
    };

    private static final DirectiveHandler HAS_NO_INLINE_METADATA = new DirectiveHandler("CHECK_HAS_NO_INLINE_METADATA") {
        @Override
        void processEntry(@NotNull JsNode ast, @NotNull ArgumentsHelper arguments) throws Exception {
            String functionName = arguments.getPositionalArgument(0);
            JsExpression property = AstSearchUtil.getProperty(ast, functionName);
            String message = "Inline metadata has been generated for not effectively public function " + functionName;
            assertTrue(message, property instanceof JsFunction);
        }
    };

    public static void processDirectives(@NotNull JsNode ast, @NotNull String sourceCode) throws Exception {
        FUNCTION_CONTAINS_NO_CALLS.process(ast, sourceCode);
        FUNCTION_NOT_CALLED.process(ast, sourceCode);
        FUNCTION_CALLED_IN_SCOPE.process(ast, sourceCode);
        FUNCTION_NOT_CALLED_IN_SCOPE.process(ast, sourceCode);
        FUNCTIONS_HAVE_SAME_LINES.process(ast, sourceCode);
        COUNT_LABELS.process(ast, sourceCode);
        COUNT_VARS.process(ast, sourceCode);
        HAS_INLINE_METADATA.process(ast, sourceCode);
        HAS_NO_INLINE_METADATA.process(ast, sourceCode);
    }

    public static void checkFunctionContainsNoCalls(JsNode node, String functionName) throws Exception {
        JsFunction function = AstSearchUtil.getFunction(node, functionName);
        CallCounter counter = CallCounter.countCalls(function);
        int callsCount = counter.getTotalCallsCount();

        String errorMessage = functionName + " contains calls";
        assertEquals(errorMessage, 0, callsCount);
    }

    public static void checkFunctionNotCalled(JsNode node, String functionName) throws Exception {
        CallCounter counter = CallCounter.countCalls(node);
        int functionCalledCount = counter.getQualifiedCallsCount(functionName);

        String errorMessage = "inline function `" + functionName + "` is called";
        assertEquals(errorMessage, 0, functionCalledCount);
    }

    public static void checkCalledInScope(
            @NotNull JsNode node,
            @NotNull String functionName,
            @NotNull String scopeFunctionName
    ) throws Exception {
        String errorMessage = functionName + " is not called inside " + scopeFunctionName;
        assertFalse(errorMessage, isCalledInScope(node, functionName, scopeFunctionName));
    }

    public static void checkNotCalledInScope(
            @NotNull JsNode node,
            @NotNull String functionName,
            @NotNull String scopeFunctionName
    ) throws Exception {
        String errorMessage = functionName + " is called inside " + scopeFunctionName;
        assertTrue(errorMessage, isCalledInScope(node, functionName, scopeFunctionName));
    }

    private static boolean isCalledInScope(
            @NotNull JsNode node,
            @NotNull String functionName,
            @NotNull String scopeFunctionName
    ) throws Exception {
        JsNode scope = AstSearchUtil.getFunction(node, scopeFunctionName);

        CallCounter counter = CallCounter.countCalls(scope);
        return counter.getQualifiedCallsCount(functionName) == 0;
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
        private final List<String> positionalArguments = new ArrayList<String>();
        private final Map<String, String> namedArguments = new HashMap<String, String>();
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
