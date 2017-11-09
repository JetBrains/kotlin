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

package org.jetbrains.kotlin.js.translate.utils.mutator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsNode;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.types.KotlinType;

public class CoercionMutator implements Mutator {
    private final KotlinType targetType;
    private final TranslationContext context;

    public CoercionMutator(@NotNull KotlinType targetType, @NotNull TranslationContext context) {
        this.targetType = targetType;
        this.context = context;
    }

    @NotNull
    @Override
    public JsNode mutate(@NotNull JsNode node) {
        if (node instanceof JsExpression) {
            return TranslationUtils.coerce(context, (JsExpression) node, targetType);
        }

        return node;
    }
}
