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

package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;

import java.util.List;

import static org.jetbrains.k2js.translate.general.Translation.translateAsStatement;
import static org.jetbrains.k2js.translate.initializer.InitializerUtils.generateInitializerForProperty;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptor;

public final class InitializerVisitor extends TranslatorVisitor<Void> {
    private final List<JsStatement> result;

    public InitializerVisitor(List<JsStatement> result) {
        this.result = result;
    }

    @Override
    public final Void visitProperty(@NotNull JetProperty property, @NotNull TranslationContext context) {
        JetExpression initializer = property.getInitializer();
        if (initializer != null) {
            result.add(generateInitializerForProperty(context, getPropertyDescriptor(context.bindingContext(), property),
                                                      Translation.translateAsExpression(initializer, context)));
        }

        return null;
    }

    @Override
    public Void visitAnonymousInitializer(@NotNull JetClassInitializer initializer, @NotNull TranslationContext context) {
        result.add(translateAsStatement(initializer.getBody(), context));
        return null;
    }

    @Override
    // Not interested in other types of declarations, they do not contain initializers.
    public Void visitDeclaration(@NotNull JetDeclaration expression, @NotNull TranslationContext context) {
        return null;
    }

    @Override
    public Void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, @NotNull TranslationContext context) {
        InitializerUtils.generate(declaration, result, null, context);
        return null;
    }
}
