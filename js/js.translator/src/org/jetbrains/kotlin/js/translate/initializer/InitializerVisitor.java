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

package org.jetbrains.kotlin.js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.general.TranslatorVisitor;
import org.jetbrains.kotlin.psi.*;

import java.util.List;

import static org.jetbrains.kotlin.js.translate.general.Translation.translateAsStatementAndMergeInBlockIfNeeded;
import static org.jetbrains.kotlin.js.translate.initializer.InitializerUtils.generateInitializerForDelegate;
import static org.jetbrains.kotlin.js.translate.initializer.InitializerUtils.generateInitializerForProperty;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getPropertyDescriptor;

public final class InitializerVisitor extends TranslatorVisitor<Void> {
    private final List<JsStatement> result;

    public InitializerVisitor(List<JsStatement> result) {
        this.result = result;
    }

    @Override
    protected Void emptyResult(@NotNull TranslationContext context) {
        return null;
    }

    @Override
    public final Void visitProperty(@NotNull KtProperty property, @NotNull TranslationContext context) {
        KtExpression initializer = property.getInitializer();
        if (initializer != null) {
            result.add(generateInitializerForProperty(context, getPropertyDescriptor(context.bindingContext(), property),
                                                      Translation.translateAsExpression(initializer, context)));
        }

        JsStatement delegate = generateInitializerForDelegate(context, property);
        if (delegate != null) result.add(delegate);

        return null;
    }

    @Override
    public Void visitAnonymousInitializer(@NotNull KtAnonymousInitializer initializer, @NotNull TranslationContext context) {
        KtExpression initializerBody = initializer.getBody();
        if (initializerBody != null) {
            result.add(translateAsStatementAndMergeInBlockIfNeeded(initializerBody, context));
        }
        return null;
    }

    @Override
    // Not interested in other types of declarations, they do not contain initializers.
    public Void visitDeclaration(@NotNull KtDeclaration expression, @NotNull TranslationContext context) {
        return null;
    }

    @Override
    public Void visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor, TranslationContext data) {
        return null;
    }
}
