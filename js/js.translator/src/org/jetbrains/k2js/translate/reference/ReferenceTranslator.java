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
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.jet.lang.psi.JetPsiUtil.isBackingFieldReference;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getSelectorAsSimpleName;

public final class ReferenceTranslator {

    private ReferenceTranslator() {
    }

    @NotNull
    public static JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression,
            @NotNull TranslationContext context) {
        return getAccessTranslator(expression, context).translateAsGet();
    }

    @NotNull
    public static JsExpression translateAsFQReference(@NotNull DeclarationDescriptor referencedDescriptor,
            @NotNull TranslationContext context) {
        JsExpression alias = context.getAliasForDescriptor(referencedDescriptor);
        return alias != null ? alias : context.getQualifiedReference(referencedDescriptor);
    }

    @NotNull
    public static JsExpression translateAsLocalNameReference(@NotNull DeclarationDescriptor descriptor,
            @NotNull TranslationContext context) {
        if (descriptor instanceof FunctionDescriptor || descriptor instanceof VariableDescriptor) {
            JsExpression alias = context.getAliasForDescriptor(descriptor);
            if (alias != null) {
                return alias;
            }
        }
        return context.getNameForDescriptor(descriptor).makeRef();
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull JetSimpleNameExpression referenceExpression,
            @NotNull TranslationContext context) {
        return getAccessTranslator(referenceExpression, null, context);
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull JetSimpleNameExpression referenceExpression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context) {
        if (isBackingFieldReference(referenceExpression)) {
            return BackingFieldAccessTranslator.newInstance(referenceExpression, context);
        }
        if (canBePropertyAccess(referenceExpression, context)) {
            return VariableAccessTranslator.newInstance(context, referenceExpression, receiver);
        }
        if (ClassObjectAccessTranslator.isClassObjectReference(referenceExpression, context)) {
            return ClassObjectAccessTranslator.newInstance(referenceExpression, context);
        }
        return ReferenceAccessTranslator.newInstance(referenceExpression, context);
    }

    public static boolean canBePropertyAccess(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        JetSimpleNameExpression simpleNameExpression = null;
        if (expression instanceof JetQualifiedExpression) {
            simpleNameExpression = getSelectorAsSimpleName((JetQualifiedExpression) expression);
        }
        else if (expression instanceof JetSimpleNameExpression) {
            simpleNameExpression = (JetSimpleNameExpression) expression;
        }

        if (simpleNameExpression == null) return false;

        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression(context.bindingContext(), simpleNameExpression);

        // Skip ValueParameterDescriptor because sometime we can miss resolved call for it, e.g. when set something to delegated property.
        return descriptor instanceof VariableDescriptor && !(descriptor instanceof ValueParameterDescriptor);
    }

}
