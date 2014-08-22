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

import static org.jetbrains.k2js.inline.FunctionInlineMutator.getInlineableCallReplacement;

public class JsInliner extends JsVisitorWithContextImpl {

    private final IdentityHashMap<JsName, JsFunction> functions;
    private final Stack<JsScope> scopeStack = new Stack<JsScope>();
    private final JsProgram program;

    public static JsProgram process(JsProgram program) {
        IdentityHashMap<JsName, JsFunction> functions = FunctionCollector.collectFunctions(program);
        JsInliner inliner = new JsInliner(program, functions);
        return inliner.process();
    }

    JsInliner(JsProgram program, IdentityHashMap<JsName, JsFunction> functions) {
        this.program = program;
        this.functions = functions;
    }

    @Override
    public boolean visit(JsFunction function, JsContext context) {
        scopeStack.push(function.getScope());
        return super.visit(function, context);
    }

    @Override
    public void endVisit(JsFunction function, JsContext context) {
        super.endVisit(function, context);
        scopeStack.pop();
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
    
    private JsProgram process() {
        return this.accept(program);
    }

    private JsScope getCurrentFunctionScope() {
        assert !scopeStack.isEmpty();
        return scopeStack.peek();
    }

    private void inline(@NotNull JsInvocation call, @NotNull JsContext context) {
        JsFunction functionToInline = findDeclaration(call);
        assert functionToInline != null;
        JsContext statementLevelContext = getLastStatementLevelContext();
        assert statementLevelContext != null;

        JsScope currentScope = getCurrentFunctionScope();
        InlineableResult inlineableResult = getInlineableCallReplacement(call, currentScope, functionToInline);

        JsStatement inlineableBody = inlineableResult.getInlineableBody();
        JsExpression resultExpression = inlineableResult.getResultExpression();

        statementLevelContext.insertAfter(statementLevelContext.getCurrentNode());
        statementLevelContext.insertAfter(inlineableBody);
        statementLevelContext.replaceMe(program.getEmptyStatement());

        context.replaceMe(resultExpression);
    }

    @Nullable
    private JsFunction findDeclaration(@NotNull JsInvocation call) {
        JsName name = getFunctionName(call);
        if (functions.containsKey(name)) {
            return functions.get(name);
        }

        if (name.getStaticRef() != null && name.getStaticRef() instanceof JsFunction) {
            return (JsFunction) name.getStaticRef();
        }

        return null;
    }

    private boolean canInline(@NotNull JsInvocation call) {
        return findDeclaration(call) != null;
    }

    private static boolean shouldInline(@NotNull JsInvocation call) {
        return call.getInlineStrategy().isInline();
    }

    @NotNull
    private static JsName getFunctionName(@NotNull JsInvocation call) {
        JsExpression qualifier = call.getQualifier();
        assert qualifier instanceof JsNameRef;

        JsName name = ((JsNameRef) qualifier).getName();
        assert name != null;

        return name;
    }
}
