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

import com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.Map;

/**
 * @author Pavel Talanov
 */
public class AliasingContext {

    @NotNull
    private static final AliasingContext ROOT = new AliasingContext(null) {
        @Override
        public JsName getAliasForThis(@NotNull DeclarationDescriptor descriptor) {
            return null;
        }

        @Override
        public JsName getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
            return null;
        }

        @Override
        public JsName getAliasForExpression(@NotNull JetExpression element) {
            return null;
        }
    };

    public static AliasingContext getCleanContext() {
        return new AliasingContext(ROOT);
    }

    @NotNull
    private final Map<DeclarationDescriptor, JsName> aliasesForDescriptors = Maps.newHashMap();
    @NotNull
    private final Map<DeclarationDescriptor, JsName> aliasesForThis = Maps.newHashMap();
    @NotNull
    private final Map<JetExpression, JsName> aliasesForExpressions = Maps.newHashMap();

    @Nullable
    private final AliasingContext parent;

    private AliasingContext(@Nullable AliasingContext parent) {
        this.parent = parent;
    }

    @NotNull
    public AliasingContext withThisAliased(@NotNull DeclarationDescriptor correspondingDescriptor, @NotNull JsName alias) {
        AliasingContext newContext = new AliasingContext(this);
        newContext.aliasesForThis.put(correspondingDescriptor, alias);
        return newContext;
    }

    @NotNull
    public AliasingContext withAliasesForExpressions(@NotNull Map<JetExpression, JsName> aliasesForExpressions) {
        AliasingContext newContext = new AliasingContext(this);
        newContext.aliasesForExpressions.putAll(aliasesForExpressions);
        return newContext;
    }

    @NotNull
    public AliasingContext withDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsName> aliases) {
        AliasingContext newContext = new AliasingContext(this);
        newContext.aliasesForDescriptors.putAll(aliases);
        return newContext;
    }

    @NotNull
    private AliasingContext getParent() {
        assert parent != null;
        return parent;
    }

    @Nullable
    public JsName getAliasForThis(@NotNull DeclarationDescriptor descriptor) {
        JsName alias = aliasesForThis.get(descriptor.getOriginal());
        if (alias != null) {
            return alias;
        }
        return getParent().getAliasForThis(descriptor);
    }

    @Nullable
    public JsName getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        JsName alias = aliasesForDescriptors.get(descriptor.getOriginal());
        if (alias != null) {
            return alias;
        }
        return getParent().getAliasForDescriptor(descriptor);
    }

    @Nullable
    public JsName getAliasForExpression(@NotNull JetExpression element) {
        JsName alias = aliasesForExpressions.get(element);
        if (alias != null) {
            return alias;
        }
        return getParent().getAliasForExpression(element);
    }
}
