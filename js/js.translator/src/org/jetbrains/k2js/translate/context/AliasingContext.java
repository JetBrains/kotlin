/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.*;

public class AliasingContext {
    private static final AliasingContext ROOT = new AliasingContext(null) {
        @Override
        public JsExpression getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
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

    @Nullable
    private Map<DeclarationDescriptor, JsExpression> aliasesForDescriptors;

    @NotNull
    private final Map<JetExpression, JsName> aliasesForExpressions = Maps.newHashMap();

    @Nullable
    private final AliasingContext parent;

    private AliasingContext(@Nullable AliasingContext parent) {
        this(parent, null);
    }

    private AliasingContext(@Nullable AliasingContext parent, @Nullable Map<DeclarationDescriptor, JsExpression> aliasesForDescriptors) {
        this.parent = parent;
        this.aliasesForDescriptors = aliasesForDescriptors;
    }

    @NotNull
    public AliasingContext inner(@NotNull DeclarationDescriptor descriptor, @NotNull JsExpression alias) {
        return new AliasingContext(this, Collections.singletonMap(descriptor, alias));
    }

    @NotNull
    public AliasingContext withAliasesForExpressions(@NotNull Map<JetExpression, JsName> aliasesForExpressions) {
        AliasingContext newContext = new AliasingContext(this);
        newContext.aliasesForExpressions.putAll(aliasesForExpressions);
        return newContext;
    }

    @NotNull
    public AliasingContext withDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsExpression> aliases) {
        return new AliasingContext(this, aliases);
    }

    @Nullable
    public JsExpression getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (aliasesForDescriptors == null) {
            return null;
        }

        JsExpression alias = aliasesForDescriptors.get(descriptor.getOriginal());
        if (alias != null) {
            return alias;
        }

        return parent.getAliasForDescriptor(descriptor);
    }

    @Nullable
    public JsName getAliasForExpression(@NotNull JetExpression element) {
        JsName alias = aliasesForExpressions.get(element);
        return alias != null ? alias : parent.getAliasForExpression(element);
    }

    public void registerAlias(@NotNull DeclarationDescriptor descriptor, @NotNull JsExpression alias) {
        if (aliasesForDescriptors == null) {
            aliasesForDescriptors = Collections.singletonMap(descriptor, alias);
        }
        else {
            if (aliasesForDescriptors.size() == 1) {
                Map<DeclarationDescriptor, JsExpression> singletonMap = aliasesForDescriptors;
                aliasesForDescriptors = new HashMap<DeclarationDescriptor, JsExpression>();
                aliasesForDescriptors.put(singletonMap.keySet().iterator().next(), singletonMap.values().iterator().next());
            }
            JsExpression prev = aliasesForDescriptors.put(descriptor, alias);
            assert prev == null;
        }
    }
}
