/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.*;

/**
 * @author Pavel Talanov
 */
public final class DeclarationBodyVisitor extends TranslatorVisitor<List<JsPropertyInitializer>> {
    @Nullable
    private final ClassDeclarationTranslator classDeclarationTranslator;

    public DeclarationBodyVisitor() {
        classDeclarationTranslator = null;
    }

    public DeclarationBodyVisitor(ClassDeclarationTranslator classDeclarationTranslator) {
        this.classDeclarationTranslator = classDeclarationTranslator;
    }

    @NotNull
    public List<JsPropertyInitializer> traverseClass(@NotNull JetClassOrObject jetClass,
                                                     @NotNull TranslationContext context) {
        List<JsPropertyInitializer> properties = new ArrayList<JsPropertyInitializer>();
        for (JetDeclaration declaration : jetClass.getDeclarations()) {
            properties.addAll(declaration.accept(this, context));
        }
        return properties;
    }

    @NotNull
    public List<JsPropertyInitializer> traverseNamespace(@NotNull NamespaceDescriptor namespace,
            @NotNull TranslationContext context) {
        List<JsPropertyInitializer> properties = new ArrayList<JsPropertyInitializer>();
        for (JetDeclaration declaration : getDeclarationsForNamespace(context.bindingContext(), namespace)) {
            properties.addAll(declaration.accept(this, context));
        }
        return properties;
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitClass(@NotNull JetClass expression, @NotNull TranslationContext context) {
        if (classDeclarationTranslator == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(classDeclarationTranslator.translateAndGetClassNameToClassObject(expression));
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitNamedFunction(@NotNull JetNamedFunction expression,
                                                          @NotNull TranslationContext context) {
        JsPropertyInitializer methodAsPropertyInitializer = Translation.functionTranslator(expression, context).translateAsMethod();
        if (context.isEcma5()) {
            FunctionDescriptor descriptor = getFunctionDescriptor(context.bindingContext(), expression);
            JsExpression methodBodyExpression = methodAsPropertyInitializer.getValueExpr();
            methodAsPropertyInitializer.setValueExpr(JsAstUtils.createPropertyDataDescriptor(descriptor, methodBodyExpression));
        }
        return Collections.singletonList(methodAsPropertyInitializer);
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitProperty(@NotNull JetProperty expression,
                                                     @NotNull TranslationContext context) {
        PropertyDescriptor propertyDescriptor =
                BindingUtils.getPropertyDescriptor(context.bindingContext(), expression);
        return PropertyTranslator.translateAccessors(propertyDescriptor, context);
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitObjectDeclaration(@NotNull JetObjectDeclaration expression,
                                                              @NotNull TranslationContext context) {
        return Collections.singletonList(ClassTranslator.translateAsProperty(expression, context));
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitObjectDeclarationName(@NotNull JetObjectDeclarationName expression,
                                                                  @NotNull TranslationContext context) {
        if (context.isEcma5()) {
            return Collections.emptyList();
        }

        PropertyDescriptor propertyDescriptor =
                getPropertyDescriptorForObjectDeclaration(context.bindingContext(), expression);
        return PropertyTranslator.translateAccessors(propertyDescriptor, context);
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitAnonymousInitializer(@NotNull JetClassInitializer expression,
                                                                 @NotNull TranslationContext context) {
        // parsed it in initializer visitor => no additional actions are needed
        return Collections.emptyList();
    }
}
