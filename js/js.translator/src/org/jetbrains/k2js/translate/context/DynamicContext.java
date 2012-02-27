/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.addVarDeclaration;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.newVar;

public class DynamicContext {

    @NotNull
    public static DynamicContext rootContext(@NotNull NamingScope rootScope, @NotNull JsBlock globalBlock) {
        return new DynamicContext(rootScope, rootScope, globalBlock);
    }

    @NotNull
    public static DynamicContext contextWithScope(@NotNull NamingScope scope) {
        return new DynamicContext(scope, scope, new JsBlock());
    }

    //TODO: current/block scope logic unclear. is it necessary?
    @NotNull
    private NamingScope currentScope;

    @NotNull
    private NamingScope blockScope;

    @NotNull
    private JsBlock currentBlock;

    private DynamicContext(@NotNull NamingScope scope, @NotNull NamingScope blockScope, @NotNull JsBlock block) {
        this.currentScope = scope;
        this.currentBlock = block;
        this.blockScope = blockScope;
    }

    @NotNull
    public DynamicContext innerScope(@NotNull JsScope scope) {
        return new DynamicContext(currentScope.innerScope(scope), blockScope, currentBlock);
    }

    @NotNull
    public NamingScope getScope() {
        return blockScope;
    }

    @NotNull
    public DynamicContext innerBlock(@NotNull JsBlock block) {
        return new DynamicContext(currentScope, currentScope, block);
    }

    @NotNull
    public TemporaryVariable declareTemporary(@NotNull JsExpression initExpression) {
        JsName temporaryName = blockScope.declareTemporary();
        JsVars temporaryDeclaration = newVar(temporaryName, /*no init expression in var statement*/ null);
        addVarDeclaration(jsBlock(), temporaryDeclaration);
        return new TemporaryVariable(temporaryName, initExpression);
    }


    @NotNull
    public JsScope jsScope() {
        return currentScope.jsScope();
    }

    @NotNull
    public JsBlock jsBlock() {
        return currentBlock;
    }
}