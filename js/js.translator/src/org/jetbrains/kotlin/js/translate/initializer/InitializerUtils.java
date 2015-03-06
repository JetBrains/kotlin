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

package org.jetbrains.kotlin.js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.declaration.ClassTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetObjectDeclaration;
import org.jetbrains.kotlin.psi.JetProperty;

import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.assignmentToBackingField;

public final class InitializerUtils {
    private InitializerUtils() {
    }

    @NotNull
    public static JsStatement generateInitializerForProperty(@NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression value) {
        return assignmentToBackingField(context, descriptor, value).makeStmt();
    }

    @Nullable
    public static JsStatement generateInitializerForDelegate(@NotNull TranslationContext context, @NotNull JetProperty property) {
        JetExpression delegate = property.getDelegateExpression();
        if (delegate != null) {
            JsExpression value = Translation.translateAsExpression(delegate, context);
            String name = property.getName();
            assert name != null: "Delegate property must have name";
            return JsAstUtils.defineSimpleProperty(Namer.getDelegateName(name), value);
        }
        return null;
    }

    public static void generateObjectInitializer(
            @NotNull JetObjectDeclaration declaration,
            @NotNull List<JsStatement> initializers,
            @NotNull TranslationContext context
    ) {
        JsExpression value = ClassTranslator.generateObjectLiteral(declaration, context);
        ClassDescriptor descriptor = getClassDescriptor(context.bindingContext(), declaration);
        JsExpression expression = assignment(new JsNameRef(descriptor.getName().asString(), JsLiteral.THIS), value);
        initializers.add(expression.makeStmt());
    }

    public static JsPropertyInitializer createDefaultObjectInitializer(JsExpression value, TranslationContext context) {
        JsStringLiteral defaultObjectInitStr = context.program().getStringLiteral(Namer.getNameForDefaultObjectInitializer());
        return new JsPropertyInitializer(defaultObjectInitStr, value);
    }
}
