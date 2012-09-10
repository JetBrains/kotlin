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

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    @Nullable
    private final Map<JetExpression, JsName> aliasesForExpressions;

    @Nullable
    private final AliasingContext parent;

    private AliasingContext(@Nullable AliasingContext parent) {
        this(parent, null, null);
    }

    private AliasingContext(
            @Nullable AliasingContext parent,
            @Nullable Map<DeclarationDescriptor, JsExpression> aliasesForDescriptors,
            @Nullable Map<JetExpression, JsName> aliasesForExpressions
    ) {
        this.parent = parent;
        this.aliasesForDescriptors = aliasesForDescriptors;
        this.aliasesForExpressions = aliasesForExpressions;
    }

    @NotNull
    public AliasingContext inner(@NotNull DeclarationDescriptor descriptor, @NotNull JsExpression alias) {
        return new AliasingContext(this, Collections.singletonMap(descriptor, alias), null);
    }

    @NotNull
    public AliasingContext withExpressionsAliased(@NotNull Map<JetExpression, JsName> aliasesForExpressions) {
        return new AliasingContext(this, null, aliasesForExpressions);
    }

    @NotNull
    public AliasingContext withDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsExpression> aliases) {
        return new AliasingContext(this, aliases, null);
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
        JsName alias = aliasesForExpressions == null ? null : aliasesForExpressions.get(element);
        return alias != null || parent == null ? alias : parent.getAliasForExpression(element);
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
