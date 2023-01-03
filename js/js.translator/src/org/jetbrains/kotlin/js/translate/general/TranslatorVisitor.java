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

package org.jetbrains.kotlin.js.translate.general;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtDeclarationContainer;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtVisitor;

/**
 * This class is a base class for all visitors.
 */
public abstract class TranslatorVisitor<T> extends KtVisitor<T, TranslationContext> {

    protected abstract T emptyResult(@NotNull TranslationContext context);

    @Override
    public T visitKtElement(@NotNull KtElement expression, TranslationContext context) {
        context.bindingTrace().report(ErrorsJs.NOT_SUPPORTED.on(expression, expression));
        return emptyResult(context);
    }

    public final void traverseContainer(@NotNull KtDeclarationContainer ktClass,
            @NotNull TranslationContext context) {
        for (KtDeclaration declaration : ktClass.getDeclarations()) {
            declaration.accept(this, context);
        }
    }
}
