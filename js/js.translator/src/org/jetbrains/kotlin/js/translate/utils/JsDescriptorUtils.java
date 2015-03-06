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

package org.jetbrains.kotlin.js.translate.utils;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig;
import org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsPackage;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.js.config.LibrarySourcesConfig.BUILTINS_JS_MODULE_NAME;
import static org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;

public final class JsDescriptorUtils {
    // TODO: maybe we should use external annotations or something else.
    private static final Set<String> FAKE_CLASSES = ContainerUtil.immutableSet(
            getFqNameSafe(KotlinBuiltIns.getInstance().getAny()).asString()
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
        return descriptor.getName().equals(OperatorConventions.COMPARE_TO);
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

                return !FAKE_CLASSES.contains(getFqNameSafe(classDescriptor).asString()) &&
                       !(classDescriptor.getKind() == ClassKind.TRAIT && isNativeObject(classDescriptor));
            }
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
        if (receiverParameter instanceof ThisReceiver) {
            DeclarationDescriptor declarationDescriptor = ((ThisReceiver) receiverParameter).getDeclarationDescriptor();
            return declarationDescriptor.getOriginal();
        }

        throw new UnsupportedOperationException("Unsupported receiver type: " + receiverParameter.getClass() +
                                                ", receiverParameter = " + receiverParameter);
    }

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
        return type != null ? DescriptorUtilsPackage.getNameIfStandardType(type) : null;
    }

    @NotNull
    public static String getModuleName(@NotNull DeclarationDescriptor descriptor) {
        String externalModuleName = getExternalModuleName(descriptor);
        if (externalModuleName != null) return externalModuleName;

        return getModuleNameFromDescriptorName(descriptor);
    }

    @Nullable
    public static String getExternalModuleName(@NotNull DeclarationDescriptor descriptor) {
        if (isBuiltin(descriptor)) return BUILTINS_JS_MODULE_NAME;

        PsiElement element = descriptorToDeclaration(descriptor);
        if (element == null && descriptor instanceof PropertyAccessorDescriptor) {
            element = descriptorToDeclaration(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty());
        }

        if (element == null) return getModuleNameFromDescriptorName(descriptor);

        return element.getContainingFile().getUserData(LibrarySourcesConfig.EXTERNAL_MODULE_NAME);
    }

    private static String getModuleNameFromDescriptorName(DeclarationDescriptor descriptor) {
        ModuleDescriptor moduleDescriptor = DescriptorUtils.getContainingModule(descriptor);
        String moduleName = moduleDescriptor.getName().asString();
        return moduleName.substring(1, moduleName.length() - 1);
    }
}
