/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.JsExpression;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.psi.JetExpression;

import java.util.Collections;
import java.util.Map;

public class AliasingContext {
    public static AliasingContext getCleanContext() {
        return new AliasingContext(null, null, null);
    }

    @Nullable
    private Map<DeclarationDescriptor, JsExpression> aliasesForDescriptors;

    @Nullable
    private final Map<JetExpression, JsExpression> aliasesForExpressions;

    @Nullable
    private final AliasingContext parent;

    private AliasingContext(
            @Nullable AliasingContext parent,
            @Nullable Map<DeclarationDescriptor, JsExpression> aliasesForDescriptors,
            @Nullable Map<JetExpression, JsExpression> aliasesForExpressions
    ) {
        this.parent = parent;
        this.aliasesForDescriptors = aliasesForDescriptors;
        this.aliasesForExpressions = aliasesForExpressions;
    }

    @NotNull
    public AliasingContext inner() {
        return new AliasingContext(this, null, null);
    }

    @NotNull
    public AliasingContext inner(@NotNull DeclarationDescriptor descriptor, @NotNull JsExpression alias) {
        return new AliasingContext(this, Collections.singletonMap(descriptor, alias), null);
    }

    @NotNull
    public AliasingContext withExpressionsAliased(@NotNull Map<JetExpression, JsExpression> aliasesForExpressions) {
        return new AliasingContext(this, null, aliasesForExpressions);
    }

    @NotNull
    public AliasingContext withDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsExpression> aliases) {
        return new AliasingContext(this, aliases, null);
    }


    @Nullable
    final public JsExpression getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        // these aliases cannot be shared and applicable only in current context
        return getAliasForDescriptor(descriptor, false);
    }

    @Nullable
    protected JsExpression getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor, boolean fromChild) {
        JsExpression alias = aliasesForDescriptors == null ? null : aliasesForDescriptors.get(descriptor.getOriginal());
        return alias != null || parent == null ? alias : parent.getAliasForDescriptor(descriptor, true);
    }

    @Nullable
    public JsExpression getAliasForExpression(@NotNull JetExpression element) {
        JsExpression alias = aliasesForExpressions == null ? null : aliasesForExpressions.get(element);
        return alias != null || parent == null ? alias : parent.getAliasForExpression(element);
    }

    /**
     * Usages:
     * 1) Local variable captured in closure. If captured in closure, any modification in closure should affect captured variable.
     * So, "var count = n" wrapped as "var count = {v: n}". descriptor wil be property descriptor, alias will be JsObjectLiteral
     *
     * 2) Local named function.
     */
    public void registerAlias(@NotNull DeclarationDescriptor descriptor, @NotNull JsExpression alias) {
        if (aliasesForDescriptors == null) {
            aliasesForDescriptors = Collections.singletonMap(descriptor, alias);
        }
        else {
            if (aliasesForDescriptors.size() == 1) {
                Map<DeclarationDescriptor, JsExpression> singletonMap = aliasesForDescriptors;
                aliasesForDescriptors = new THashMap<DeclarationDescriptor, JsExpression>();
                aliasesForDescriptors.put(singletonMap.keySet().iterator().next(), singletonMap.values().iterator().next());
            }
            JsExpression prev = aliasesForDescriptors.put(descriptor, alias);
            assert prev == null : "Alias for descriptor already registered." +
                                  " Descriptor: " + descriptor +
                                  " prev alias: " + prev +
                                  " new alias: " + alias;
        }
    }
}
