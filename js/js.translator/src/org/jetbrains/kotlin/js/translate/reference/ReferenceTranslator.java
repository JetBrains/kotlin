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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsInvocation;
import org.jetbrains.kotlin.js.backend.ast.JsName;
import org.jetbrains.kotlin.js.backend.ast.JsNameRef;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind;
import org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsKt;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForTypeAliasObject;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.jetbrains.kotlin.descriptors.DescriptorPsiUtilsKt.isBackingFieldReference;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getSelectorAsSimpleName;

public final class ReferenceTranslator {
    private static final Set<FqNameUnsafe> DECLARATIONS_WITHOUT_SIDE_EFFECTS = new HashSet<>(Arrays.asList(
            new FqNameUnsafe("kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED"),
            new FqNameUnsafe("kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED"),
            KotlinBuiltIns.FQ_NAMES.unit
    ));

    private ReferenceTranslator() {
    }

    @NotNull
    public static JsExpression translateSimpleName(@NotNull KtSimpleNameExpression expression, @NotNull TranslationContext context) {
        return getAccessTranslator(expression, context).translateAsGet();
    }

    @NotNull
    public static JsExpression translateAsValueReference(@NotNull DeclarationDescriptor descriptor, @NotNull TranslationContext context) {
        JsExpression result = translateAsValueReferenceWithoutType(descriptor, context);
        MetadataProperties.setType(result, getType(descriptor));
        if (isValueWithoutSideEffect(descriptor)) {
            MetadataProperties.setUnit(result, true);
            MetadataProperties.setSideEffects(result, SideEffectKind.PURE);
            MetadataProperties.setSynthetic(result, true);
        }
        return result;
    }

    @Nullable
    private static KotlinType getType(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            return ((ClassDescriptor) descriptor).getDefaultType();
        }
        else if (descriptor instanceof CallableDescriptor) {
            if (descriptor instanceof ValueParameterDescriptor) {
                ValueParameterDescriptor parameter = (ValueParameterDescriptor) descriptor;
                if (parameter.getContainingDeclaration() instanceof AnonymousFunctionDescriptor) {
                    return DescriptorUtils.getContainingModule(descriptor).getBuiltIns().getAnyType();
                }
                if (parameter.getContainingDeclaration() instanceof PropertySetterDescriptor) {
                    PropertySetterDescriptor setter = (PropertySetterDescriptor) parameter.getContainingDeclaration();
                    return TranslationUtils.getReturnTypeForCoercion(setter.getCorrespondingProperty(), false);
                }
            }
            return ((CallableDescriptor) descriptor).getReturnType();
        }

        return null;
    }

    @NotNull
    private static JsExpression translateAsValueReferenceWithoutType(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        if (AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor)) {
            return context.getInnerReference(descriptor);
        }

        JsExpression alias = context.getAliasForDescriptor(descriptor);
        if (alias != null) return alias;

        if (shouldTranslateAsFQN(descriptor)) {
            return context.getQualifiedReference(descriptor);
        }

        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor property = (PropertyDescriptor) descriptor;
            if (isLocallyAvailableDeclaration(context, property) || isValueWithoutSideEffect(property)) {
                return context.getInnerReference(property);
            }
            else {
                JsExpression qualifier = context.getInnerReference(property.getContainingDeclaration());
                JsName name = context.getNameForDescriptor(property);
                return new JsNameRef(name, qualifier);
            }
        }

        if (DescriptorUtils.isObject(descriptor) || DescriptorUtils.isEnumEntry(descriptor)) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            if (!isLocallyAvailableDeclaration(context, descriptor)) {
                if (isValueWithoutSideEffect(classDescriptor)) {
                    return context.getInnerReference(descriptor);
                }
                else {
                    return getLazyReferenceToObject(classDescriptor, context);
                }
            }
            else {
                JsExpression functionRef = JsAstUtils.pureFqn(context.getNameForObjectInstance(classDescriptor), null);
                return new JsInvocation(functionRef);
            }
        }

        return context.getInnerReference(descriptor);
    }

    private static boolean isValueWithoutSideEffect(@NotNull DeclarationDescriptor descriptor) {
        return DECLARATIONS_WITHOUT_SIDE_EFFECTS.contains(DescriptorUtils.getFqName(descriptor));
    }

    @NotNull
    public static JsExpression translateAsTypeReference(@NotNull ClassDescriptor descriptor, @NotNull TranslationContext context) {
        if (AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor)) {
            return context.getInnerReference(descriptor);
        }
        if (DescriptorUtils.isObject(descriptor) || DescriptorUtils.isEnumEntry(descriptor)) {
            if (!isLocallyAvailableDeclaration(context, descriptor)) {
                return getPrototypeIfNecessary(descriptor, getLazyReferenceToObject(descriptor, context));
            }
        }
        return context.getInnerReference(descriptor);
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

    private static boolean isLocallyAvailableDeclaration(@NotNull TranslationContext context, @NotNull DeclarationDescriptor descriptor) {
        return context.isFromCurrentModule(descriptor) && !(context.isPublicInlineFunction() &&
               DescriptorUtilsKt.shouldBeExported(descriptor, context.getConfig()));
    }

    @NotNull
    private static JsExpression getLazyReferenceToObject(@NotNull ClassDescriptor descriptor, @NotNull TranslationContext context) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        JsExpression qualifier = context.getInnerReference(container);
        return new JsNameRef(context.getNameForDescriptor(descriptor), qualifier);
    }

    private static boolean shouldTranslateAsFQN(@NotNull DeclarationDescriptor descriptor) {
        return isLocalVarOrFunction(descriptor);
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
        return descriptor instanceof VariableDescriptor &&
               !(descriptor instanceof ValueParameterDescriptor) &&
               !(descriptor instanceof FakeCallableDescriptorForTypeAliasObject);
    }

}
