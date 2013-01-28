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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import static org.jetbrains.k2js.translate.utils.AnnotationsUtils.isNativeObject;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCallForProperty;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getSelectorAsSimpleName;
import static org.jetbrains.jet.lang.psi.JetPsiUtil.isBackingFieldReference;

public abstract class PropertyAccessTranslator extends AbstractTranslator implements AccessTranslator {

    @NotNull
    public static PropertyAccessTranslator newInstance(@NotNull JetSimpleNameExpression expression,
            @Nullable JsExpression qualifier,
            @NotNull CallType callType,
            @NotNull TranslationContext context) {
        PropertyAccessTranslator result;
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(expression, context);
        if (isNativeObject(propertyDescriptor) || isBackingFieldReference(expression)) {
            result = new NativePropertyAccessTranslator(propertyDescriptor, qualifier, context);
        }
        else {
            ResolvedCall<?> resolvedCall = getResolvedCallForProperty(context.bindingContext(), expression);
            result = new KotlinPropertyAccessTranslator(propertyDescriptor, qualifier, resolvedCall, context);
        }
        result.setCallType(callType);
        return result;
    }

    @NotNull
    private static PropertyDescriptor getPropertyDescriptor(@NotNull JetSimpleNameExpression expression,
            @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor =
                getDescriptorForReferenceExpression(context.bindingContext(), expression);
        assert descriptor instanceof PropertyDescriptor : "Must be a property descriptor.";
        return (PropertyDescriptor) descriptor;
    }


    @NotNull
    public static JsExpression translateAsPropertyGetterCall(@NotNull JetSimpleNameExpression expression,
            @Nullable JsExpression qualifier,
            @NotNull CallType callType,
            @NotNull TranslationContext context) {
        return (newInstance(expression, qualifier, callType, context))
                .translateAsGet();
    }


    private static boolean canBePropertyGetterCall(@NotNull JetQualifiedExpression expression,
            @NotNull TranslationContext context) {
        JetSimpleNameExpression selector = getSelectorAsSimpleName(expression);
        assert selector != null : "Only names are allowed after the dot";
        return canBePropertyGetterCall(selector, context);
    }

    private static boolean canBePropertyGetterCall(@NotNull JetSimpleNameExpression expression,
            @NotNull TranslationContext context) {
        return (getDescriptorForReferenceExpression
                        (context.bindingContext(), expression) instanceof PropertyDescriptor);
    }

    public static boolean canBePropertyGetterCall(@NotNull JetExpression expression,
            @NotNull TranslationContext context) {
        if (expression instanceof JetQualifiedExpression) {
            return canBePropertyGetterCall((JetQualifiedExpression) expression, context);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return canBePropertyGetterCall((JetSimpleNameExpression) expression, context);
        }
        return false;
    }

    public static boolean canBePropertyAccess(@NotNull JetExpression expression,
            @NotNull TranslationContext context) {
        return canBePropertyGetterCall(expression, context);
    }

    //TODO: we use normal by default but may cause bugs
    //TODO: inspect
    private /*var*/ CallType callType = CallType.NORMAL;

    protected PropertyAccessTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    public void setCallType(@NotNull CallType callType) {
        this.callType = callType;
    }

    @NotNull
    protected CallType getCallType() {
        assert callType != null : "CallType not set";
        return callType;
    }

    @NotNull
    protected abstract JsExpression translateAsGet(@Nullable JsExpression receiver);

    @NotNull
    protected abstract JsExpression translateAsSet(@Nullable JsExpression receiver, @NotNull JsExpression setTo);
}