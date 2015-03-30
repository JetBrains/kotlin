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

package org.jetbrains.kotlin.js.inline;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.metadata.MetadataPackage;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.DiagnosticSink;
import org.jetbrains.kotlin.js.inline.context.*;
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.resolve.inline.InlineStrategy;

import java.util.*;

import static org.jetbrains.kotlin.js.inline.FunctionInlineMutator.getInlineableCallReplacement;
import static org.jetbrains.kotlin.js.inline.clean.CleanPackage.removeUnusedFunctionDefinitions;
import static org.jetbrains.kotlin.js.inline.clean.CleanPackage.removeUnusedLocalFunctionDeclarations;
import static org.jetbrains.kotlin.js.inline.util.UtilPackage.*;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.flattenStatement;

public class JsInliner extends JsVisitorWithContextImpl {

    private final IdentityHashMap<JsName, JsFunction> functions;
    private final Stack<JsInliningContext> inliningContexts = new Stack<JsInliningContext>();
    private final Set<JsFunction> processedFunctions = IdentitySet();
    private final Set<JsFunction> inProcessFunctions = IdentitySet();
    private final FunctionReader functionReader;
    private final DiagnosticSink trace;

    // these are needed for error reporting, when inliner detects cycle
    private final Stack<JsFunction> namedFunctionsStack = new Stack<JsFunction>();
    private final LinkedList<JsCallInfo> inlineCallInfos = new LinkedList<JsCallInfo>();

    public static JsProgram process(@NotNull TranslationContext context) {
        JsProgram program = context.program();
        IdentityHashMap<JsName, JsFunction> functions = collectNamedFunctions(program);
        JsInliner inliner = new JsInliner(functions, new FunctionReader(context), context.bindingTrace());
        inliner.accept(program);
        removeUnusedFunctionDefinitions(program, functions);
        return program;
    }

    private JsInliner(
            @NotNull IdentityHashMap<JsName, JsFunction> functions,
            @NotNull FunctionReader functionReader,
            @NotNull DiagnosticSink trace
    ) {
        this.functions = functions;
        this.functionReader = functionReader;
        this.trace = trace;
    }

    @Override
    public boolean visit(@NotNull JsFunction function, @NotNull JsContext context) {
        inliningContexts.push(new JsInliningContext(function));
        assert !inProcessFunctions.contains(function): "Inliner has revisited function";
        inProcessFunctions.add(function);

        if (functions.containsValue(function)) {
            namedFunctionsStack.push(function);
        }

        return super.visit(function, context);
    }

    @Override
    public void endVisit(@NotNull JsFunction function, @NotNull JsContext context) {
        super.endVisit(function, context);
        refreshLabelNames(getInliningContext().newNamingContext(), function);

        removeUnusedLocalFunctionDeclarations(function);
        processedFunctions.add(function);

        assert inProcessFunctions.contains(function);
        inProcessFunctions.remove(function);

        inliningContexts.pop();

        if (!namedFunctionsStack.empty() && namedFunctionsStack.peek() == function) {
            namedFunctionsStack.pop();
        }
    }

    @Override
    public boolean visit(@NotNull JsInvocation call, @NotNull JsContext context) {
        if (shouldInline(call) && canInline(call)) {
            JsFunction containingFunction = getCurrentNamedFunction();
            if (containingFunction != null) {
                inlineCallInfos.add(new JsCallInfo(call, containingFunction));
            }

            JsFunction definition = getFunctionContext().getFunctionDefinition(call);

            if (inProcessFunctions.contains(definition))  {
                reportInlineCycle(call, definition);
                return false;
            }

            if (!processedFunctions.contains(definition)) {
                accept(definition);
            }

            inline(call, context);
        }

        return true;
    }

    @Override
    public void endVisit(@NotNull JsInvocation x, @NotNull JsContext ctx) {
        JsCallInfo lastCallInfo = null;

        if (!inlineCallInfos.isEmpty()) {
            lastCallInfo = inlineCallInfos.getLast();
        }

        if (lastCallInfo != null && lastCallInfo.call == x) {
            inlineCallInfos.removeLast();
        }
    }

    private void inline(@NotNull JsInvocation call, @NotNull JsContext context) {
        JsInliningContext inliningContext = getInliningContext();
        FunctionContext functionContext = getFunctionContext();
        functionContext.declareFunctionConstructorCalls(call.getArguments());
        InlineableResult inlineableResult = getInlineableCallReplacement(call, inliningContext);

        JsStatement inlineableBody = inlineableResult.getInlineableBody();
        JsExpression resultExpression = inlineableResult.getResultExpression();
        JsContext<JsStatement> statementContext = inliningContext.getStatementContext();
        // body of inline function can contain call to lambdas that need to be inlined
        accept(inlineableBody);
        statementContext.addPrevious(flattenStatement(inlineableBody));

        /**
         * Assumes, that resultExpression == null, when result is not needed.
         * @see FunctionInlineMutator.isResultNeeded()
         */
        if (resultExpression == null) {
            statementContext.removeMe();
            return;
        }

        resultExpression = accept(resultExpression);
        context.replaceMe(resultExpression);
    }

    @NotNull
    private JsInliningContext getInliningContext() {
        return inliningContexts.peek();
    }

    @NotNull FunctionContext getFunctionContext() {
        return getInliningContext().getFunctionContext();
    }

    @Nullable
    private JsFunction getCurrentNamedFunction() {
        if (namedFunctionsStack.empty()) return null;
        return namedFunctionsStack.peek();
    }

    private void reportInlineCycle(@NotNull JsInvocation call, @NotNull JsFunction calledFunction) {
        MetadataPackage.setInlineStrategy(call, InlineStrategy.NOT_INLINE);
        Iterator<JsCallInfo> it = inlineCallInfos.descendingIterator();

        while (it.hasNext()) {
            JsCallInfo callInfo = it.next();
            PsiElement psiElement = MetadataPackage.getPsiElement(callInfo.call);

            if (psiElement != null) {
                trace.report(ErrorsJs.INLINE_CALL_CYCLE.on(psiElement));
            }

            if (callInfo.containingFunction == calledFunction) {
                break;
            }
        }
    }

    private boolean canInline(@NotNull JsInvocation call) {
        FunctionContext functionContext = getFunctionContext();
        return functionContext.hasFunctionDefinition(call);
    }

    private static boolean shouldInline(@NotNull JsInvocation call) {
        InlineStrategy strategy = MetadataPackage.getInlineStrategy(call);
        return strategy != null && strategy.isInline();
    }

    private class JsInliningContext implements InliningContext {
        private final FunctionContext functionContext;

        JsInliningContext(JsFunction function) {
            functionContext = new FunctionContext(function, this, functionReader) {
                @Nullable
                @Override
                protected JsFunction lookUpStaticFunction(@Nullable JsName functionName) {
                    return functions.get(functionName);
                }
            };
        }

        @NotNull
        @Override
        public NamingContext newNamingContext() {
            JsScope scope = getFunctionContext().getScope();
            return new NamingContext(scope, getStatementContext());
        }

        @NotNull
        @Override
        public JsContext<JsStatement> getStatementContext() {
            return getLastStatementLevelContext();
        }

        @NotNull
        @Override
        public FunctionContext getFunctionContext() {
            return functionContext;
        }
    }

    private static class JsCallInfo {
        @NotNull
        public final JsInvocation call;

        @NotNull
        public final JsFunction containingFunction;

        private JsCallInfo(@NotNull JsInvocation call, @NotNull JsFunction function) {
            this.call = call;
            containingFunction = function;
        }
    }
}
