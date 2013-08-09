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

package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.initializer.InitializerUtils.createPropertyInitializer;
import static org.jetbrains.k2js.translate.utils.BindingUtils.*;

public class DeclarationBodyVisitor extends TranslatorVisitor<Void> {
    protected final List<JsPropertyInitializer> result;
    protected final List<JsPropertyInitializer> staticResult;

    public DeclarationBodyVisitor() {
        this(new SmartList<JsPropertyInitializer>(), new SmartList<JsPropertyInitializer>());
    }

    public DeclarationBodyVisitor(@NotNull List<JsPropertyInitializer> result, @NotNull List<JsPropertyInitializer> staticResult) {
        this.result = result;
        this.staticResult = staticResult;
    }

    @NotNull
    public List<JsPropertyInitializer> getResult() {
        return result;
    }

    @Override
    public Void visitClass(@NotNull JetClass expression, @NotNull TranslationContext context) {
        return null;
    }

    @Override
    public Void visitClassObject(
            JetClassObject classObject, TranslationContext context
    ) {
        JetObjectDeclaration declaration = classObject.getObjectDeclaration();
        assert declaration != null : "Declaration for class object must be not null";
        ClassDescriptor descriptor = getClassDescriptor(context.bindingContext(), declaration);
        JsExpression value = ClassTranslator.generateClassCreation(declaration, descriptor, context);

        JsFunction fun = new JsFunction(context.getScopeForDescriptor(descriptor.getContainingDeclaration()));
        fun.setBody(new JsBlock(new JsReturn(value)));
        staticResult.add(createPropertyInitializer(Namer.getNamedForClassObjectInitializer(), fun, context));
        return null;
    }

    @Override
    public Void visitNamedFunction(@NotNull JetNamedFunction expression, @NotNull TranslationContext context) {
        FunctionDescriptor descriptor = getFunctionDescriptor(context.bindingContext(), expression);
        if (descriptor.getModality() == Modality.ABSTRACT) {
            return null;
        }

        JsPropertyInitializer methodAsPropertyInitializer = Translation.functionTranslator(expression, context).translateAsMethod();
        if (context.isEcma5()) {
            JsExpression methodBodyExpression = methodAsPropertyInitializer.getValueExpr();
            methodAsPropertyInitializer.setValueExpr(JsAstUtils.createPropertyDataDescriptor(descriptor, methodBodyExpression));
        }
        result.add(methodAsPropertyInitializer);
        return null;
    }

    @Override
    public Void visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        PropertyDescriptor propertyDescriptor = BindingUtils.getPropertyDescriptor(context.bindingContext(), expression);
        PropertyTranslator.translateAccessors(propertyDescriptor, expression, result, context);
        return null;
    }

    @Override
    public Void visitObjectDeclarationName(@NotNull JetObjectDeclarationName expression,
            @NotNull TranslationContext context) {
        if (!context.isEcma5()) {
            PropertyTranslator
                    .translateAccessors(getPropertyDescriptorForObjectDeclaration(context.bindingContext(), expression), result, context);
        }

        return null;
    }

    @Override
    public Void visitAnonymousInitializer(@NotNull JetClassInitializer expression, @NotNull TranslationContext context) {
        // parsed it in initializer visitor => no additional actions are needed
        return null;
    }
}
