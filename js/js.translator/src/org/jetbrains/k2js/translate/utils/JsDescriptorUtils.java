/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.k2js.config.LibrarySourcesConfig;

import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getSuperclassDescriptors;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isClassObject;

/**
 * @author Pavel Talanov
 */
public final class JsDescriptorUtils {

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

    public static boolean isConstructorDescriptor(@NotNull CallableDescriptor descriptor) {
        return (descriptor instanceof ConstructorDescriptor);
    }

    @Nullable
    public static ClassDescriptor findAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
        for (ClassDescriptor descriptor : superclassDescriptors) {
            if (descriptor.getKind() == ClassKind.CLASS) {
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
    public static DeclarationDescriptor getContainingDeclaration(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        assert containing != null : "Should be called on objects that have containing declaration.";
        return containing;
    }

    public static boolean isExtension(@NotNull CallableDescriptor functionDescriptor) {
        return (functionDescriptor.getReceiverParameter().exists());
    }

    //TODO: why callable descriptor
    @Nullable
    public static DeclarationDescriptor getExpectedThisDescriptor(@NotNull CallableDescriptor callableDescriptor) {
        ReceiverDescriptor expectedThisObject = callableDescriptor.getExpectedThisObject();
        if (!expectedThisObject.exists()) {
            return null;
        }
        return getDeclarationDescriptorForReceiver(expectedThisObject);
    }

    @NotNull
    public static DeclarationDescriptor getDeclarationDescriptorForReceiver
            (@NotNull ReceiverDescriptor receiverParameter) {
        DeclarationDescriptor declarationDescriptor =
                receiverParameter.getType().getConstructor().getDeclarationDescriptor();
        //TODO: WHY assert?
        assert declarationDescriptor != null;
        return declarationDescriptor.getOriginal();
    }

    @Nullable
    public static DeclarationDescriptor getExpectedReceiverDescriptor(@NotNull CallableDescriptor callableDescriptor) {
        ReceiverDescriptor receiverParameter = callableDescriptor.getReceiverParameter();
        if (!receiverParameter.exists()) {
            return null;
        }
        return getDeclarationDescriptorForReceiver(receiverParameter);
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
    public static NamespaceDescriptor getContainingNamespace(@NotNull DeclarationDescriptor descriptor) {
        return DescriptorUtils.getParentOfType(descriptor, NamespaceDescriptor.class);
    }

    public static boolean isStandardDeclaration(@NotNull DeclarationDescriptor descriptor) {
        NamespaceDescriptor namespace = getContainingNamespace(descriptor);
        if (namespace == null) {
            return false;
        }
        return namespace.equals(JetStandardLibrary.getInstance().getLibraryScope().getContainingDeclaration());
    }

    @NotNull
    public static List<NamespaceDescriptor> getNestedNamespaces(@NotNull NamespaceDescriptor namespaceDescriptor,
            @NotNull BindingContext context) {
        List<NamespaceDescriptor> result = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : getContainedDescriptorsWhichAreNotPredefined(namespaceDescriptor, context)) {
            if (descriptor instanceof NamespaceDescriptor) {
                result.add((NamespaceDescriptor) descriptor);
            }
        }
        return result;
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

    @NotNull
    public static List<DeclarationDescriptor> getContainedDescriptorsWhichAreNotPredefined(@NotNull NamespaceDescriptor namespace,
            @NotNull BindingContext context) {
        List<DeclarationDescriptor> result = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : namespace.getMemberScope().getAllDescriptors()) {
            if (!AnnotationsUtils.isPredefinedObject(descriptor)) {
                // namespace may be defined in multiple files
                if (!(descriptor instanceof NamespaceDescriptor)) {
                    PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(context, descriptor);
                    if (psiElement != null) {
                        PsiFile file = psiElement.getContainingFile();
                        if (file.getUserData(LibrarySourcesConfig.EXTERNAL_MODULE_NAME) != null) {
                            continue;
                        }
                    }
                }

                result.add(descriptor);
            }
        }
        return result;
    }

    //TODO: at the moment this check is very ineffective
    public static boolean isNamespaceEmpty(@NotNull NamespaceDescriptor namespace, @NotNull BindingContext context) {
        List<DeclarationDescriptor> containedDescriptors = getContainedDescriptorsWhichAreNotPredefined(namespace, context);
        for (DeclarationDescriptor descriptor : containedDescriptors) {
            if (descriptor instanceof NamespaceDescriptor) {
                if (!isNamespaceEmpty((NamespaceDescriptor) descriptor, context)) {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public static List<NamespaceDescriptor> getNamespaceDescriptorHierarchy(@NotNull NamespaceDescriptor namespaceDescriptor) {
        List<NamespaceDescriptor> result = Lists.newArrayList(namespaceDescriptor);
        NamespaceDescriptor current = namespaceDescriptor;
        while (!(current.getContainingDeclaration() instanceof ModuleDescriptor)) {
            result.add(current);
            if (current.getContainingDeclaration() instanceof NamespaceDescriptor) {
                current = (NamespaceDescriptor) current.getContainingDeclaration();
                //noinspection ConstantConditions
                assert current != null;
            }
            else {
                break;
            }
        }
        return result;
    }

    private static boolean isDefaultAccessor(@Nullable PropertyAccessorDescriptor accessorDescriptor) {
        return accessorDescriptor == null || accessorDescriptor.isDefault();
    }

    public static boolean isAsPrivate(@NotNull PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.getReceiverParameter().exists() ||
               !isDefaultAccessor(propertyDescriptor.getGetter()) ||
               !isDefaultAccessor(propertyDescriptor.getSetter());
    }

    @NotNull
    public static DeclarationDescriptor getDeclarationDescriptorForExtensionCallReceiver(
            @NotNull ResolvedCall<? extends CallableDescriptor> resolvedCall
    ) {
        ReceiverDescriptor receiverArgument = resolvedCall.getReceiverArgument();
        if (receiverArgument instanceof ExtensionReceiver) {
            return ((ExtensionReceiver) receiverArgument).getDeclarationDescriptor();
        }
        if (receiverArgument instanceof ClassReceiver) {
            return ((ClassReceiver)receiverArgument).getDeclarationDescriptor();
        }
        throw new IllegalStateException("Unexpected receiver of type " + receiverArgument.getClass());
    }
}
