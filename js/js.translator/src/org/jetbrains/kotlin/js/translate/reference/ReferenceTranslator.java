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

package org.jetbrains.kotlin.js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetQualifiedExpression;
import org.jetbrains.kotlin.psi.JetSimpleNameExpression;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.setQualifier;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getSelectorAsSimpleName;
import static org.jetbrains.kotlin.psi.JetPsiUtil.isBackingFieldReference;

public final class ReferenceTranslator {

    private ReferenceTranslator() {
    }

    @NotNull
    public static JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression,
            @NotNull TranslationContext context) {
        return getAccessTranslator(expression, context).translateAsGet();
    }

    @NotNull
    public static JsExpression translateSimpleNameWithQualifier(
            @NotNull JetSimpleNameExpression expression,
            @Nullable JsExpression qualifier,
            @NotNull TranslationContext context
    ) {
        JsExpression simpleName = translateSimpleName(expression, context);

        // Ignore qualifier if expression is EnumEntry or default object reference and always use FQ name.
        DeclarationDescriptor descriptor = BindingUtils.getDescriptorForReferenceExpression(context.bindingContext(), expression);
        //TODO: should go away when objects inside classes are supported
        if (DescriptorUtils.isDefaultObject(descriptor) && !AnnotationsUtils.isNativeObject(descriptor)) {
            return simpleName;
        }
        if (descriptor instanceof ClassDescriptor) {
            ClassDescriptor entryClass = (ClassDescriptor) descriptor;
            if (entryClass.getKind() == ClassKind.ENUM_ENTRY && !AnnotationsUtils.isNativeObject(entryClass)) {
                DeclarationDescriptor enumClass = entryClass.getContainingDeclaration();
                qualifier = Namer.getDefaultObjectAccessor(translateAsFQReference(enumClass, context));
            }
        }

        if (qualifier != null) { // TODO: hack for nested Object
            setQualifier(simpleName, qualifier);
        }

        return simpleName;
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
        if (DefaultObjectAccessTranslator.isDefaultObjectReference(referenceExpression, context)) {
            return DefaultObjectAccessTranslator.newInstance(referenceExpression, context);
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
