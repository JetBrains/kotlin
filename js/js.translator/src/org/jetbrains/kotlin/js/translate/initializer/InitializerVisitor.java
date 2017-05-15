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

package org.jetbrains.kotlin.js.translate.initializer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.declaration.PropertyTranslatorKt;
import org.jetbrains.kotlin.js.translate.general.TranslatorVisitor;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.js.translate.general.Translation.translateAsStatementAndMergeInBlockIfNeeded;
import static org.jetbrains.kotlin.js.translate.initializer.InitializerUtils.generateInitializerForDelegate;
import static org.jetbrains.kotlin.js.translate.initializer.InitializerUtils.generateInitializerForProperty;

public final class InitializerVisitor extends TranslatorVisitor<Void> {
    @Override
    protected Void emptyResult(@NotNull TranslationContext context) {
        return null;
    }

    @Override
    public final Void visitProperty(@NotNull KtProperty property, @NotNull TranslationContext context) {
        PropertyDescriptor descriptor = BindingUtils.getPropertyDescriptor(context.bindingContext(), property);
        JsExpression value = PropertyTranslatorKt.translateDelegateOrInitializerExpression(context, property);
        JsStatement statement = null;

        KtExpression initializer = property.getInitializer();
        KtExpression delegate = property.getDelegateExpression();
        if (initializer != null) {
            assert value != null;
            statement = generateInitializerForProperty(context, descriptor, value);
        }
        else if (delegate != null) {
            assert value != null;
            statement = generateInitializerForDelegate(descriptor, value);
        }
        else if (Boolean.TRUE.equals(context.bindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, descriptor))) {
            JsNameRef backingFieldReference = TranslationUtils.backingFieldReference(context, descriptor);
            JsExpression defaultValue = generateDefaultValue(descriptor, backingFieldReference);
            statement = TranslationUtils.assignmentToBackingField(context, descriptor, defaultValue).source(property).makeStmt();
        }
        else if (JsDescriptorUtils.isSimpleFinalProperty(descriptor)) {
            JsNameRef propRef = new JsNameRef(context.getNameForDescriptor(descriptor), new JsThisRef());
            JsExpression defaultValue = generateDefaultValue(descriptor, propRef);
            statement = JsAstUtils.assignment(propRef, defaultValue).source(property).makeStmt();
        }

        if (statement != null && !JsAstUtils.isEmptyStatement(statement)) {
            context.addStatementsToCurrentBlock(JsAstUtils.flattenStatement(statement));
        }

        return null;
    }

    @NotNull
    private static JsExpression generateDefaultValue(
            @NotNull PropertyDescriptor property,
            @NotNull JsExpression lateInitDefault
    ) {
        if (property.isLateInit()) return lateInitDefault.deepCopy();

        KotlinType type = property.getType();
        if (KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isFloat(type) || KotlinBuiltIns.isDouble(type) ||
            KotlinBuiltIns.isByte(type) || KotlinBuiltIns.isShort(type)
        ) {
            return new JsIntLiteral(0);
        }
        else if (KotlinBuiltIns.isBoolean(type)) {
            return new JsBooleanLiteral(false);
        }
        else {
            return new JsNullLiteral();
        }
    }

    @Override
    public Void visitAnonymousInitializer(@NotNull KtAnonymousInitializer initializer, @NotNull TranslationContext context) {
        KtExpression initializerBody = initializer.getBody();
        if (initializerBody != null) {
            context.addStatementsToCurrentBlock(JsAstUtils.flattenStatement(
                    translateAsStatementAndMergeInBlockIfNeeded(initializerBody, context)));
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
