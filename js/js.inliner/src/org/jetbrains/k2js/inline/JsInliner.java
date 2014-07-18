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

    private JsProgram process() {
        return this.accept(program);
    }

    private JsScope getCurrentFunctionScope() {
        assert !scopeStack.isEmpty();
        return scopeStack.peek();
    }
}
