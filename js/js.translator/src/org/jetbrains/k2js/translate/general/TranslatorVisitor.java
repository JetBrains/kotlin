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

package org.jetbrains.k2js.translate.general;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;

/**
 * This class is a base class for all visitors.
 */
public class TranslatorVisitor<T> extends JetVisitor<T, TranslationContext> {

    @Override
    @NotNull
    public T visitJetElement(JetElement expression, TranslationContext context) {
        throw new UnsupportedOperationException("Unsupported expression encountered:" + expression.toString());
    }

    public final void traverseContainer(@NotNull JetDeclarationContainer jetClass,
            @NotNull TranslationContext context) {
        for (JetDeclaration declaration : jetClass.getDeclarations()) {
            declaration.accept(this, context);
        }
    }
}
