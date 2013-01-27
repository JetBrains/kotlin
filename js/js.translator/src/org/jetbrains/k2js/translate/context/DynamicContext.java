/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.google.dart.compiler.backend.js.ast.JsVars.JsVar;

//TODO: consider renaming to scoping context
public final class DynamicContext {
    @NotNull
    public static DynamicContext rootContext(@NotNull JsScope rootScope, @NotNull JsBlock globalBlock) {
        return new DynamicContext(rootScope, globalBlock);
    }

    @NotNull
    public static DynamicContext newContext(@NotNull JsScope scope, @NotNull JsBlock block) {
        return new DynamicContext(scope, block);
    }

    @NotNull
    private final JsScope currentScope;

    @NotNull
    private final JsBlock currentBlock;

    @Nullable
    private JsVars vars;

    private DynamicContext(@NotNull JsScope scope, @NotNull JsBlock block) {
        this.currentScope = scope;
        this.currentBlock = block;
    }

    @NotNull
    public DynamicContext innerBlock(@NotNull JsBlock block) {
        return new DynamicContext(currentScope, block);
    }

    @NotNull
    public TemporaryVariable declareTemporary(@Nullable JsExpression initExpression) {
        if (vars == null) {
            vars = new JsVars();
            currentBlock.getStatements().add(vars);
        }

        JsName temporaryName = currentScope.declareTemporary();
        vars.add(new JsVar(temporaryName, null));
        return new TemporaryVariable(temporaryName, initExpression);
    }

    @NotNull
    public Pair<JsVar, JsExpression> createTemporary(@Nullable JsExpression initExpression) {
        JsVar var = new JsVar(currentScope.declareTemporary(), initExpression);
        return new Pair<JsVar, JsExpression>(var, var.getName().makeRef());
    }

    @NotNull
    public JsVar createTemporaryVar(@NotNull JsExpression initExpression) {
        return new JsVar(currentScope.declareTemporary(), initExpression);
    }

    @NotNull
    public JsScope getScope() {
        return currentScope;
    }

    @NotNull
    public JsBlock jsBlock() {
        return currentBlock;
    }
}