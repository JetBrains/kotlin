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

import java.util.IdentityHashMap;
import java.util.Stack;
import java.util.List;

import static org.jetbrains.k2js.inline.FunctionInlineMutator.getInlineableCallReplacement;

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

    public static JsProgram process(JsProgram program) {
        IdentityHashMap<JsName, JsFunction> functions = FunctionCollector.collectFunctions(program);
        JsInliner inliner = new JsInliner(functions);
        return inliner.accept(program);
    }

    JsInliner(IdentityHashMap<JsName, JsFunction> functions) {
        this.functions = functions;
    }

    @Override
    public boolean visit(JsFunction function, JsContext context) {
        inliningContexts.push(new JsInliningContext(function));
        return super.visit(function, context);
    }

    @Override
    public void endVisit(JsFunction function, JsContext context) {
        super.endVisit(function, context);
        inliningContexts.pop();
    }

    @Override
    public void endVisit(JsInvocation call, JsContext context) {
        super.endVisit(call, context);

        if (call == null) {
            return;
        }

        if (shouldInline(call) && canInline(call)) {
            inline(call, context);
        }
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

        statementContext.shiftCurrentStatementForward();
        InsertionPoint<JsStatement> insertionPoint = statementContext.getInsertionPoint();
        if (inlineableBody instanceof JsBlock) {
            JsBlock block = (JsBlock) inlineableBody;
            insertionPoint.insertAllAfter(block.getStatements());
        } else {
            insertionPoint.insertAfter(inlineableBody);
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
