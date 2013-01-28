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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

public final class CachedPropertyAccessTranslator implements CachedAccessTranslator {

    @NotNull
    private final PropertyAccessTranslator baseTranslator;
    @Nullable
    private final TemporaryVariable cachedReceiver;

    /*package*/ CachedPropertyAccessTranslator(@Nullable JsExpression receiverExpression,
                                               @NotNull PropertyAccessTranslator baseTranslator,
                                               @NotNull TranslationContext context) {
        this.cachedReceiver = receiverExpression != null ? context.declareTemporary(receiverExpression) : null;
        this.baseTranslator = baseTranslator;
    }

    @NotNull
    @Override
    public JsExpression translateAsGet() {
        return baseTranslator.translateAsGet(receiverOrNull());
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression toSetTo) {
        return baseTranslator.translateAsSet(receiverOrNull(), toSetTo);
    }

    @Nullable
    private JsNameRef receiverOrNull() {
        return cachedReceiver != null ? cachedReceiver.reference() : null;
    }

    @NotNull
    @Override
    public CachedAccessTranslator getCached() {
        return this;
    }

    @NotNull
    @Override
    public List<TemporaryVariable> declaredTemporaries() {
        return cachedReceiver != null ? singletonList(cachedReceiver) : Collections.<TemporaryVariable>emptyList();
    }
}
