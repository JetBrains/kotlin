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

package org.jetbrains.kotlin.js.translate.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;

import static org.jetbrains.kotlin.js.backend.ast.JsVars.JsVar;

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
    public TemporaryVariable declareTemporary(@Nullable JsExpression initExpression, @Nullable Object sourceInfo) {
        if (vars == null) {
            vars = new JsVars();
            MetadataProperties.setSynthetic(vars, true);
            currentBlock.getStatements().add(vars);
            vars.setSource(sourceInfo);
        }

        JsName temporaryName = JsScope.declareTemporary();
        JsVar var = new JsVar(temporaryName, null);
        var.setSource(sourceInfo);
        MetadataProperties.setSynthetic(var, true);
        vars.add(var);
        if (initExpression != null) {
            var.source(initExpression.getSource());
        }
        return TemporaryVariable.create(temporaryName, initExpression);
    }

    void moveVarsFrom(@NotNull DynamicContext dynamicContext) {
        if (dynamicContext.vars != null) {
            if (vars == null) {
                vars = dynamicContext.vars;
                currentBlock.getStatements().add(vars);
            } else {
                vars.addAll(dynamicContext.vars);
            }
            dynamicContext.currentBlock.getStatements().remove(dynamicContext.vars);
            dynamicContext.vars = null;
        }
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
