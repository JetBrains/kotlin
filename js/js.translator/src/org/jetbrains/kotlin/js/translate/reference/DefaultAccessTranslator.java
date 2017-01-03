/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.reference;

import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.psi.KtExpression;

public class DefaultAccessTranslator implements AccessTranslator {
    private final KtExpression expression;
    private final TranslationContext context;

    public DefaultAccessTranslator(@NotNull KtExpression expression, @NotNull TranslationContext context) {
        this.expression = expression;
        this.context = context;
    }

    @NotNull
    @Override
    public JsExpression translateAsGet() {
        return Translation.translateAsExpression(expression, context);
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        throw new UnsupportedOperationException("This method is not meant to be supported in in DefaultAccessTranslator");
    }

    @NotNull
    @Override
    public AccessTranslator getCached() {
        throw new UnsupportedOperationException("This method is not meant to be supported in in DefaultAccessTranslator");
    }
}
