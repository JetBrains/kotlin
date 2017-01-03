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

import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.psi.KtExpression;

import java.util.Collections;
import java.util.Map;

public class AliasingContext {
    @NotNull
    public static AliasingContext getCleanContext() {
        return new AliasingContext(null, null, null);
    }

    @Nullable
    private final Map<DeclarationDescriptor, JsExpression> aliasesForDescriptors;

    @Nullable
    private final Map<KtExpression, JsExpression> aliasesForExpressions;

    @Nullable
    private final AliasingContext parent;

    private AliasingContext(
            @Nullable AliasingContext parent,
            @Nullable Map<DeclarationDescriptor, JsExpression> aliasesForDescriptors,
            @Nullable Map<KtExpression, JsExpression> aliasesForExpressions
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
    public AliasingContext withExpressionsAliased(@NotNull Map<KtExpression, JsExpression> aliasesForExpressions) {
        return new AliasingContext(this, null, aliasesForExpressions);
    }

    @NotNull
    public AliasingContext withDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsExpression> aliases) {
        return new AliasingContext(this, aliases, null);
    }

    @Nullable
    public JsExpression getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        // these aliases cannot be shared and applicable only in current context
        JsExpression alias = aliasesForDescriptors != null ? aliasesForDescriptors.get(descriptor.getOriginal()) : null;
        return alias != null || parent == null ? alias : parent.getAliasForDescriptor(descriptor);
    }

    @Nullable
    public JsExpression getAliasForExpression(@NotNull KtExpression element) {
        JsExpression alias = aliasesForExpressions != null ? aliasesForExpressions.get(element) : null;
        return alias != null || parent == null ? alias : parent.getAliasForExpression(element);
    }
}
