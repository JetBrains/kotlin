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

package org.jetbrains.kotlin.js.translate.utils;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallsKt;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;

public final class JsDescriptorUtils {
    // TODO: maybe we should use external annotations or something else.
    private static final Set<String> FAKE_CLASSES = ContainerUtil.immutableSet(
            KotlinBuiltIns.FQ_NAMES.any.asString()
    );

    private JsDescriptorUtils() {
    }

    private static int valueParametersCount(@NotNull FunctionDescriptor functionDescriptor) {
        return functionDescriptor.getValueParameters().size();
    }

    public static boolean hasParameters(@NotNull FunctionDescriptor functionDescriptor) {
        return (valueParametersCount(functionDescriptor) > 0);
    }

    public static boolean isCompareTo(@NotNull CallableDescriptor descriptor) {
        return descriptor.getName().equals(OperatorNameConventions.COMPARE_TO);
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
    public static List<KotlinType> getSupertypesWithoutFakes(ClassDescriptor descriptor) {
        Collection<KotlinType> supertypes = descriptor.getTypeConstructor().getSupertypes();
        return ContainerUtil.filter(supertypes, type -> {
            ClassDescriptor classDescriptor = getClassDescriptorForType(type);

            return !FAKE_CLASSES.contains(getFqNameSafe(classDescriptor).asString()) &&
                   !(classDescriptor.getKind() == ClassKind.INTERFACE && isNativeObject(classDescriptor));
        });
    }

    @NotNull
    public static DeclarationDescriptor getContainingDeclaration(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        assert containing != null : "Should be called on objects that have containing declaration.";
        return containing;
    }

    @NotNull
    public static ReceiverParameterDescriptor getReceiverParameterForReceiver(@NotNull ReceiverValue receiverParameter) {
        DeclarationDescriptor declarationDescriptor = getDeclarationDescriptorForReceiver(receiverParameter);
        return getReceiverParameterForDeclaration(declarationDescriptor);
    }

    @NotNull
    private static DeclarationDescriptor getDeclarationDescriptorForReceiver(@NotNull ReceiverValue receiverParameter) {
        if (receiverParameter instanceof ImplicitReceiver) {
            DeclarationDescriptor declarationDescriptor = ((ImplicitReceiver) receiverParameter).getDeclarationDescriptor();
            return declarationDescriptor.getOriginal();
        }

        throw new UnsupportedOperationException("Unsupported receiver type: " + receiverParameter.getClass() +
                                                ", receiverParameter = " + receiverParameter);
    }

    @NotNull
    public static ReceiverParameterDescriptor getReceiverParameterForDeclaration(DeclarationDescriptor declarationDescriptor) {
        if (declarationDescriptor instanceof ClassDescriptor) {
            return ((ClassDescriptor) declarationDescriptor).getThisAsReceiverParameter();
        }
        else if (declarationDescriptor instanceof CallableMemberDescriptor) {
            ReceiverParameterDescriptor receiverDescriptor = ((CallableMemberDescriptor) declarationDescriptor).getExtensionReceiverParameter();
            assert receiverDescriptor != null;
            return receiverDescriptor;
        }

        throw new UnsupportedOperationException("Unsupported declaration type: " + declarationDescriptor.getClass() +
                                                ", declarationDescriptor = " + declarationDescriptor);
    }

    private static boolean isDefaultAccessor(@Nullable PropertyAccessorDescriptor accessorDescriptor) {
        return accessorDescriptor == null || accessorDescriptor.isDefault() &&
               !(accessorDescriptor instanceof PropertySetterDescriptor && accessorDescriptor.getCorrespondingProperty().isLateInit());
    }

    public static boolean sideEffectsPossibleOnRead(@NotNull PropertyDescriptor property) {
        return DynamicCallsKt.isDynamic(property) || !isDefaultAccessor(property.getGetter()) ||
               ModalityKt.isOverridableOrOverrides(property) || isStaticInitializationPossible(property);
    }

    private static boolean isStaticInitializationPossible(PropertyDescriptor property) {
        DeclarationDescriptor container = property.getContainingDeclaration();
        return container instanceof PackageFragmentDescriptor || DescriptorUtils.isObject(container);
    }

    public static boolean isSimpleFinalProperty(@NotNull PropertyDescriptor propertyDescriptor) {
        return !isExtension(propertyDescriptor) &&
               isDefaultAccessor(propertyDescriptor.getGetter()) &&
               isDefaultAccessor(propertyDescriptor.getSetter()) &&
               !TranslationUtils.shouldAccessViaFunctions(propertyDescriptor) &&
               !ModalityKt.isOverridableOrOverrides(propertyDescriptor);
    }

    @NotNull
    public static String getModuleName(@NotNull DeclarationDescriptor descriptor) {
        ModuleDescriptor moduleDescriptor = DescriptorUtils.getContainingModule(findRealInlineDeclaration(descriptor));
        if (DescriptorUtils.getContainingModule(descriptor) == moduleDescriptor.getBuiltIns().getBuiltInsModule()) {
            return Namer.KOTLIN_LOWER_NAME;
        }
        String moduleName = moduleDescriptor.getName().asString();
        return moduleName.substring(1, moduleName.length() - 1);
    }

    @NotNull
    public static DeclarationDescriptor findRealInlineDeclaration(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor d = (FunctionDescriptor) descriptor;
            if (d.getKind().isReal() || !d.isInline()) return descriptor;
            CallableMemberDescriptor real = findRealDeclaration(d);
            assert real != null : "Couldn't find definition of a fake inline descriptor " + descriptor;
            return real;
        }
        return descriptor;
    }

    @Nullable
    private static FunctionDescriptor findRealDeclaration(@NotNull FunctionDescriptor descriptor) {
        if (descriptor.getModality() == Modality.ABSTRACT) return null;
        if (descriptor.getKind().isReal()) return descriptor;

        for (FunctionDescriptor o : descriptor.getOverriddenDescriptors()) {
            FunctionDescriptor child = findRealDeclaration(o);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    public static boolean isImmediateSubtypeOfError(@NotNull ClassDescriptor descriptor) {
        if (!isExceptionClass(descriptor)) return false;
        ClassDescriptor superClass = DescriptorUtilsKt.getSuperClassOrAny(descriptor);
        return TypeUtilsKt.isNotNullThrowable(superClass.getDefaultType()) || AnnotationsUtils.isNativeObject(superClass);
    }

    public static boolean isExceptionClass(@NotNull ClassDescriptor descriptor) {
        ModuleDescriptor module = DescriptorUtils.getContainingModule(descriptor);
        return TypeUtilsKt.isSubtypeOf(descriptor.getDefaultType(), module.getBuiltIns().getThrowable().getDefaultType());
    }
}
