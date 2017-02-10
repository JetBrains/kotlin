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

package org.jetbrains.kotlin.js.translate.reference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsInvocation;
import org.jetbrains.kotlin.js.backend.ast.JsName;
import org.jetbrains.kotlin.js.backend.ast.JsNameRef;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getSelectorAsSimpleName;
import static org.jetbrains.kotlin.psi.KtPsiUtil.isBackingFieldReference;

public final class ReferenceTranslator {

    private ReferenceTranslator() {
    }

    @NotNull
    public static JsExpression translateSimpleName(@NotNull KtSimpleNameExpression expression, @NotNull TranslationContext context) {
        return getAccessTranslator(expression, context).translateAsGet();
    }

    @NotNull
    public static JsExpression translateAsValueReference(@NotNull DeclarationDescriptor descriptor, @NotNull TranslationContext context) {
        if (AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor)) {
            return context.getInnerReference(descriptor);
        }

        JsExpression alias = context.getAliasForDescriptor(descriptor);
        if (alias != null) return alias;

        if (shouldTranslateAsFQN(descriptor, context)) {
            return context.getQualifiedReference(descriptor);
        }

        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor property = (PropertyDescriptor) descriptor;
            if (context.isFromCurrentModule(property)) {
                return context.getInnerReference(property);
            }
            else {
                JsExpression qualifier = context.getInnerReference(property.getContainingDeclaration());
                JsName name = context.getNameForDescriptor(property);
                return new JsNameRef(name, qualifier);
            }
        }

        if (DescriptorUtils.isObject(descriptor) || DescriptorUtils.isEnumEntry(descriptor)) {
            if (!context.isFromCurrentModule(descriptor)) {
                return getLazyReferenceToObject((ClassDescriptor) descriptor, context);
            }
            else {
                JsExpression functionRef = JsAstUtils.pureFqn(context.getNameForObjectInstance((ClassDescriptor) descriptor), null);
                return new JsInvocation(functionRef);
            }
        }

        return context.getInnerReference(descriptor);
    }

    @NotNull
    public static JsExpression translateAsTypeReference(@NotNull ClassDescriptor descriptor, @NotNull TranslationContext context) {
        if (AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor)) {
            return context.getInnerReference(descriptor);
        }
        if (!shouldTranslateAsFQN(descriptor, context)) {
            if (DescriptorUtils.isObject(descriptor) || DescriptorUtils.isEnumEntry(descriptor)) {
                if (!context.isFromCurrentModule(descriptor)) {
                    return getPrototypeIfNecessary(descriptor, getLazyReferenceToObject(descriptor, context));
                }
            }
            return context.getInnerReference(descriptor);
        }

        return getPrototypeIfNecessary(descriptor, context.getQualifiedReference(descriptor));
    }

    @NotNull
    private static JsExpression getPrototypeIfNecessary(@NotNull ClassDescriptor descriptor, @NotNull JsExpression reference) {
        if (DescriptorUtils.isObject(descriptor) || DescriptorUtils.isEnumEntry(descriptor)) {
            JsNameRef getPrototypeRef = JsAstUtils.pureFqn("getPrototypeOf", JsAstUtils.pureFqn("Object", null));
            JsInvocation getPrototypeInvocation = new JsInvocation(getPrototypeRef, reference);
            MetadataProperties.setSideEffects(getPrototypeInvocation, SideEffectKind.PURE);
            reference = JsAstUtils.pureFqn("constructor", getPrototypeInvocation);
        }
        return reference;
    }

    @NotNull
    private static JsExpression getLazyReferenceToObject(@NotNull ClassDescriptor descriptor, @NotNull TranslationContext context) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        JsExpression qualifier = context.getInnerReference(container);
        return new JsNameRef(context.getNameForDescriptor(descriptor), qualifier);
    }

    private static boolean shouldTranslateAsFQN(@NotNull DeclarationDescriptor descriptor, @NotNull TranslationContext context) {
        return isLocalVarOrFunction(descriptor) || context.isPublicInlineFunction();
    }

    private static boolean isLocalVarOrFunction(DeclarationDescriptor descriptor) {
        return descriptor.getContainingDeclaration() instanceof FunctionDescriptor && !(descriptor instanceof ClassDescriptor);
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull KtSimpleNameExpression referenceExpression,
            @NotNull TranslationContext context) {
        if (isBackingFieldReference(getDescriptorForReferenceExpression(context.bindingContext(), referenceExpression))) {
            return BackingFieldAccessTranslator.newInstance(referenceExpression, context);
        }
        if (canBePropertyAccess(referenceExpression, context)) {
            return VariableAccessTranslator.newInstance(context, referenceExpression, null);
        }
        if (CompanionObjectIntrinsicAccessTranslator.isCompanionObjectReference(referenceExpression, context)) {
            return CompanionObjectIntrinsicAccessTranslator.newInstance(referenceExpression, context);
        }
        return ReferenceAccessTranslator.newInstance(referenceExpression, context);
    }

    public static boolean canBePropertyAccess(@NotNull KtExpression expression, @NotNull TranslationContext context) {
        KtSimpleNameExpression simpleNameExpression = null;
        if (expression instanceof KtQualifiedExpression) {
            simpleNameExpression = getSelectorAsSimpleName((KtQualifiedExpression) expression);
        }
        else if (expression instanceof KtSimpleNameExpression) {
            simpleNameExpression = (KtSimpleNameExpression) expression;
        }

        if (simpleNameExpression == null) return false;

        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression(context.bindingContext(), simpleNameExpression);

        // Skip ValueParameterDescriptor because sometime we can miss resolved call for it, e.g. when set something to delegated property.
        return descriptor instanceof VariableDescriptor && !(descriptor instanceof ValueParameterDescriptor);
    }

}
