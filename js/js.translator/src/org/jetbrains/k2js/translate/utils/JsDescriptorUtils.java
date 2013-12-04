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

package org.jetbrains.k2js.translate.utils;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqName;

public final class JsDescriptorUtils {
    // TODO: maybe we should use external annotations or something else.
    private static final Set<String> FAKE_CLASSES = ContainerUtil.immutableSet(
            getFqNameSafe(KotlinBuiltIns.getInstance().getAny()).asString(),
            "jet.Iterable"
    );

    private JsDescriptorUtils() {
    }

    private static int valueParametersCount(@NotNull FunctionDescriptor functionDescriptor) {
        return functionDescriptor.getValueParameters().size();
    }

    public static boolean hasParameters(@NotNull FunctionDescriptor functionDescriptor) {
        return (valueParametersCount(functionDescriptor) > 0);
    }

    public static boolean isCompareTo(@NotNull FunctionDescriptor functionDescriptor) {
        return (functionDescriptor.getName().equals(OperatorConventions.COMPARE_TO));
    }

    @Nullable
    public static ClassDescriptor findAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
        for (ClassDescriptor descriptor : superclassDescriptors) {
            if (descriptor.getKind() == ClassKind.CLASS || descriptor.getKind() == ClassKind.ENUM_CLASS) {
                return descriptor;
            }
        }
        return null;
    }

    @Nullable
    public static ClassDescriptor getSuperclass(@NotNull ClassDescriptor classDescriptor) {
        return findAncestorClass(getSuperclassDescriptors(classDescriptor));
    }

    @NotNull
    public static List<JetType> getSupertypesWithoutFakes(ClassDescriptor descriptor) {
        Collection<JetType> supertypes = descriptor.getTypeConstructor().getSupertypes();
        return ContainerUtil.filter(supertypes, new Condition<JetType>() {
            @Override
            public boolean value(JetType type) {
                ClassDescriptor classDescriptor = getClassDescriptorForType(type);
                return !FAKE_CLASSES.contains(getFqNameSafe(classDescriptor).asString());
            }
        });
    }

    @NotNull
    public static DeclarationDescriptor getContainingDeclaration(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        assert containing != null : "Should be called on objects that have containing declaration.";
        return containing;
    }

    public static boolean isExtension(@NotNull CallableDescriptor functionDescriptor) {
        return (functionDescriptor.getReceiverParameter() != null);
    }

    //TODO: why callable descriptor
    @Nullable
    public static DeclarationDescriptor getExpectedThisDescriptor(@NotNull CallableDescriptor callableDescriptor) {
        ReceiverParameterDescriptor expectedThisObject = callableDescriptor.getExpectedThisObject();
        if (expectedThisObject == null) {
            return null;
        }
        return getDeclarationDescriptorForReceiver(expectedThisObject.getValue());
    }

    @NotNull
    public static DeclarationDescriptor getDeclarationDescriptorForReceiver
            (@NotNull ReceiverValue receiverParameter) {
        DeclarationDescriptor declarationDescriptor =
                receiverParameter.getType().getConstructor().getDeclarationDescriptor();
        //TODO: WHY assert?
        assert declarationDescriptor != null;
        return declarationDescriptor.getOriginal();
    }

    @Nullable
    public static DeclarationDescriptor getExpectedReceiverDescriptor(@NotNull CallableDescriptor callableDescriptor) {
        ReceiverParameterDescriptor receiverParameter = callableDescriptor.getReceiverParameter();
        if (receiverParameter == null) {
            return null;
        }
        return getDeclarationDescriptorForReceiver(receiverParameter.getValue());
    }

    //TODO: maybe we have similar routine
    @Nullable
    public static ClassDescriptor getContainingClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        while (containing != null) {
            if (containing instanceof ClassDescriptor && !isClassObject(containing)) {
                return (ClassDescriptor) containing;
            }
            containing = containing.getContainingDeclaration();
        }
        return null;
    }

    @Nullable
    public static FunctionDescriptor getOverriddenDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
        Set<? extends FunctionDescriptor> overriddenDescriptors = functionDescriptor.getOverriddenDescriptors();
        if (overriddenDescriptors.isEmpty()) {
            return null;
        }
        else {
            //TODO: for now translator can't deal with multiple inheritance good enough
            return overriddenDescriptors.iterator().next();
        }
    }

    private static boolean isDefaultAccessor(@Nullable PropertyAccessorDescriptor accessorDescriptor) {
        return accessorDescriptor == null || accessorDescriptor.isDefault();
    }

    public static boolean isSimpleFinalProperty(@NotNull PropertyDescriptor propertyDescriptor) {
        return !isExtension(propertyDescriptor) &&
               isDefaultAccessor(propertyDescriptor.getGetter()) &&
               isDefaultAccessor(propertyDescriptor.getSetter()) &&
               !propertyDescriptor.getModality().isOverridable();
    }

    public static boolean isBuiltin(@NotNull DeclarationDescriptor descriptor) {
        PackageFragmentDescriptor containingPackageFragment = DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor.class);
        return containingPackageFragment == KotlinBuiltIns.getInstance().getBuiltInsPackageFragment();
    }

    @Nullable
    public static Name getNameIfStandardType(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        JetType type = context.bindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
        return type != null ? getNameIfStandardType(type) : null;
    }

    @Nullable
    public static Name getNameIfStandardType(@NotNull JetType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (descriptor != null && descriptor.getContainingDeclaration() == KotlinBuiltIns.getInstance().getBuiltInsPackageFragment()) {
            return descriptor.getName();
        }

        return null;
    }
}
