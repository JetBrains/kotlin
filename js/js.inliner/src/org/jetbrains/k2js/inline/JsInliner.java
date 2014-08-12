/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.inline;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.inline.exception.InlineRecursionException;

import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Stack;
import java.util.List;

import static org.jetbrains.k2js.inline.clean.CleanPackage.removeUnusedLocalFunctionInstances;
import static org.jetbrains.k2js.inline.clean.CleanPackage.removeUnusedLocalFunctions;
import static org.jetbrains.k2js.inline.FunctionInlineMutator.getInlineableCallReplacement;
import static org.jetbrains.k2js.inline.util.UtilPackage.IdentitySet;

public class JsInliner extends JsVisitorWithContextImpl {

    private class JsInliningContext implements InliningContext {
        private final FunctionContext functionContext;

        JsInliningContext(JsFunction function) {
            functionContext = new FunctionContext(function, this) {
                @Nullable
                @Override
                protected JsFunction lookUpStaticFunction(@Nullable JsName functionName) {
                    return functions.get(functionName);
                }
            };
        }

        @NotNull
        @Override
        public <T extends JsNode> RenamingContext<T> getRenamingContext() {
            return new RenamingContext<T>(getFunctionContext().getScope());
        }

        @NotNull
        @Override
        public StatementContext getStatementContext() {
            return new StatementContext() {
                @NotNull
                @Override
                public JsContext getCurrentStatementContext() {
                    return getLastStatementLevelContext();
                }

                @NotNull
                @Override
                protected JsStatement getEmptyStatement() {
                    return getFunctionContext().getEmpty();
                }

                @Override
                public void shiftCurrentStatementForward() {
                    super.shiftCurrentStatementForward();
                    lastStatementWasShifted = true;
                }
            };
        }

        @NotNull
        @Override
        public FunctionContext getFunctionContext() {
            return functionContext;
        }

        @NotNull
        @Override
        public JsExpression getThisReplacement(JsInvocation call) {
            if (InvocationUtil.isCallInvocation(call)) {
                return call.getArguments().get(0);
            }

            if (InvocationUtil.hasReceiver(call)) {
                return InvocationUtil.getReceiver(call);
            }

            return JsLiteral.THIS;
        }

        @NotNull
        @Override
        public List<JsExpression> getArguments(JsInvocation call) {
            List<JsExpression> arguments = call.getArguments();
            if (InvocationUtil.isCallInvocation(call)) {
                return arguments.subList(1, arguments.size());
            }

            return arguments;
        }

        @Override
        public boolean isResultNeeded(JsInvocation call) {
            JsStatement currentStatement = getStatementContext().getCurrentStatement();
            return InvocationUtil.isResultUsed(currentStatement, call);
        }
    }

    private final IdentityHashMap<JsName, JsFunction> functions;
    private final Stack<JsInliningContext> inliningContexts = new Stack<JsInliningContext>();
    private final Set<JsFunction> processedFunctions = IdentitySet();
    private final Set<JsFunction> inProcessFunctions = IdentitySet();

    /**
     * A statement can contain more, than one inlineable sub-expressions.
     * When inline call is expanded, current statement is shifted forward,
     * but still has same statement context with same index on stack.
     *
     * The shifting is intentional, because there could be function literals,
     * that need to be inlined, after expansion.
     *
     * After shifting following inline expansion in the same statement could be
     * incorrect, because wrong statement index is used.
     *
     * To prevent this, after every shift this flag is set to true,
     * so that visitor wont go deeper until statement is visited.
     *
     * Example:
     *  inline fun f(g: () -> Int): Int { val a = g(); return a }
     *  inline fun Int.abs(): Int = if (this < 0) -this else this
     *
     *  val g = { 10 }
     *  >> val h = f(g).abs()    // last statement context index
     *
     *  val g = { 10 }           // after inline
     *  >> val f$result          // statement index was not changed
     *  val a = g()
     *  f$result = a
     *  val h = f$result.abs()   // current expression still here; incorrect to inline abs(),
     *                           //  because statement context on stack point to different statement
     */
    private boolean lastStatementWasShifted = false;

    public static JsProgram process(JsProgram program) {
        IdentityHashMap<JsName, JsFunction> functions = FunctionCollector.collectFunctions(program);
        JsInliner inliner = new JsInliner(functions);
        inliner.accept(program);
        removeUnusedLocalFunctions(program, functions);
        return program;
    }

    JsInliner(IdentityHashMap<JsName, JsFunction> functions) {
        this.functions = functions;
    }

    @Override
    public boolean visit(JsFunction function, JsContext context) {
        inliningContexts.push(new JsInliningContext(function));

        if (inProcessFunctions.contains(function)) throw new InlineRecursionException();
        inProcessFunctions.add(function);

        return super.visit(function, context);
    }

    @Override
    public void endVisit(JsFunction function, JsContext context) {
        super.endVisit(function, context);
        removeUnusedLocalFunctionInstances(function);
        processedFunctions.add(function);

        assert inProcessFunctions.contains(function);
        inProcessFunctions.remove(function);

        inliningContexts.pop();
    }

    @Override
    public boolean visit(JsInvocation call, JsContext context) {
        if (call == null) {
            return false;
        }

        if (shouldInline(call) && canInline(call)) {
            JsFunction definition = getFunctionContext().getFunctionDefinition(call);
            if (!processedFunctions.contains(definition)) {
                accept(definition);
            }

            inline(call, context);
        }

        return !lastStatementWasShifted;
    }

    private void inline(@NotNull JsInvocation call, @NotNull JsContext context) {
        JsInliningContext inliningContext = getInliningContext();
        FunctionContext functionContext = getFunctionContext();
        functionContext.declareFunctionConstructorCalls(call.getArguments());
        InlineableResult inlineableResult = getInlineableCallReplacement(call, inliningContext);

        JsStatement inlineableBody = inlineableResult.getInlineableBody();
        JsExpression resultExpression = inlineableResult.getResultExpression();
        StatementContext statementContext = inliningContext.getStatementContext();

        /**
         * Assumes, that resultExpression == null, when result is not needed.
         * @see FunctionInlineMutator.isResultNeeded()
         */
        if (resultExpression == null) {
            statementContext.removeCurrentStatement();
        } else {
            context.replaceMe(resultExpression);
        }

        /** @see #lastStatementWasShifted */
        statementContext.shiftCurrentStatementForward();
        InsertionPoint<JsStatement> insertionPoint = statementContext.getInsertionPoint();
        if (inlineableBody instanceof JsBlock) {
            JsBlock block = (JsBlock) inlineableBody;
            insertionPoint.insertAllAfter(block.getStatements());
        } else {
            insertionPoint.insertAfter(inlineableBody);
        }
    }

    /**
     * Prevents JsInliner from traversing sub-expressions,
     * when current statement was shifted forward.
     */
    @Override
    protected <T extends JsNode> void doTraverse(T node, JsContext ctx) {
        if (node instanceof JsStatement) {
            /** @see #lastStatementWasShifted */
            lastStatementWasShifted = false;
        }

        if (!lastStatementWasShifted) {
            super.doTraverse(node, ctx);
        }
    }

    @NotNull
    private JsInliningContext getInliningContext() {
        return inliningContexts.peek();
    }

    @NotNull FunctionContext getFunctionContext() {
        return getInliningContext().getFunctionContext();
    }

    private boolean canInline(@NotNull JsInvocation call) {
        FunctionContext functionContext = getFunctionContext();
        return functionContext.hasFunctionDefinition(call);
    }

    private static boolean shouldInline(@NotNull JsInvocation call) {
        return call.getInlineStrategy().isInline();
    }
}
