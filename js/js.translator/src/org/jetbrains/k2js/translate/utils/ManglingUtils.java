/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.backend.common.CodegenUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqName;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.getJetTypeFqName;

public class ManglingUtils {
    private ManglingUtils() {}

    public static final Comparator<CallableMemberDescriptor> OVERLOADED_MEMBER_COMPARATOR = new OverloadedMemberComparator();

    @NotNull
    public static String getMangledName(@NotNull PropertyDescriptor descriptor, @NotNull String suggestedName) {
        return getStableMangledName(suggestedName, getFqName(descriptor).asString());
    }

    @NotNull
    public static String getSuggestedName(@NotNull DeclarationDescriptor descriptor) {
        String suggestedName = descriptor.getName().asString();

        if (descriptor instanceof FunctionDescriptor ||
            descriptor instanceof PropertyDescriptor && JsDescriptorUtils.isExtension((PropertyDescriptor) descriptor)
        ) {
            suggestedName = getMangledName((CallableMemberDescriptor) descriptor);
        }

        return suggestedName;
    }

    @NotNull
    private static String getMangledName(@NotNull CallableMemberDescriptor descriptor) {
        if (needsStableMangling(descriptor)) {
            return getStableMangledName(descriptor);
        }

        return getSimpleMangledName(descriptor);
    }

    //TODO extend logic for nested/inner declarations
    private static boolean needsStableMangling(CallableMemberDescriptor descriptor) {
        // Use stable mangling for overrides because we use stable mangling when any function inside a overridable declaration
        // for avoid clashing names when inheritance.
        if (JsDescriptorUtils.isOverride(descriptor)) {
            return true;
        }

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();

        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            return descriptor.getVisibility().isPublicAPI();
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

            // Use stable mangling when it's inside an overridable declaration to avoid clashing names on inheritance.
            if (classDescriptor.getModality().isOverridable()) {
                return true;
            }

            // valueOf() is created in the library with a mangled name for every enum class
            if (descriptor instanceof FunctionDescriptor && CodegenUtil.isEnumValueOfMethod((FunctionDescriptor) descriptor)) {
                return true;
            }

            // Don't use stable mangling when it inside a non-public API declaration.
            if (!classDescriptor.getVisibility().isPublicAPI()) {
                return false;
            }

            // Ignore the `protected` visibility because it can be use outside a containing declaration
            // only when the containing declaration is overridable.
            if (descriptor.getVisibility() == Visibilities.PUBLIC) {
                return true;
            }

            return false;
        }

        assert containingDeclaration instanceof CallableMemberDescriptor :
                "containingDeclaration for descriptor have unsupported type for mangling, " +
                "descriptor: " + descriptor + ", containingDeclaration: " + containingDeclaration;

        return false;
    }

    @NotNull
    public static String getMangledMemberNameForExplicitDelegation(@NotNull String suggestedName, FqName classFqName, FqName typeFqName) {
        String forCalculateId = classFqName.asString() + ":" + typeFqName.asString();
        return getStableMangledName(suggestedName, forCalculateId);
    }

    @NotNull
    private static String getStableMangledName(@NotNull String suggestedName, String forCalculateId) {
        int absHashCode = Math.abs(forCalculateId.hashCode());
        String suffix = absHashCode == 0 ? "" : ("_" + Integer.toString(absHashCode, Character.MAX_RADIX) + "$");
        return suggestedName + suffix;
    }

    @NotNull
    private static String getStableMangledName(@NotNull CallableDescriptor descriptor) {
        return getStableMangledName(descriptor.getName().asString(), getArgumentTypesAsString(descriptor));
    }

    @NotNull
    private static String getSimpleMangledName(@NotNull final CallableMemberDescriptor descriptor) {
        DeclarationDescriptor declaration = descriptor.getContainingDeclaration();

        JetScope jetScope = null;
        if (declaration instanceof PackageFragmentDescriptor) {
            jetScope = ((PackageFragmentDescriptor) declaration).getMemberScope();
        }
        else if (declaration instanceof ClassDescriptor) {
            jetScope = ((ClassDescriptor) declaration).getDefaultType().getMemberScope();
        }

        int counter = 0;

        if (jetScope != null) {
            Collection<DeclarationDescriptor> declarations = jetScope.getAllDescriptors();
            List<CallableMemberDescriptor>
                    overloadedFunctions = ContainerUtil.mapNotNull(declarations, new Function<DeclarationDescriptor, CallableMemberDescriptor>() {
                @Override
                public CallableMemberDescriptor fun(DeclarationDescriptor declarationDescriptor) {
                    if (!(declarationDescriptor instanceof CallableMemberDescriptor)) return null;

                    CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) declarationDescriptor;

                    String name = AnnotationsUtils.getNameForAnnotatedObjectWithOverrides(callableMemberDescriptor);

                    // when name == null it's mean that it's not native.
                    if (name == null) {
                        // skip functions without arguments, because we don't use mangling for them
                        if (needsStableMangling(callableMemberDescriptor) && !callableMemberDescriptor.getValueParameters().isEmpty()) return null;

                        // TODO add prefix for property: get_$name and set_$name
                        name = callableMemberDescriptor.getName().asString();
                    }

                    return descriptor.getName().asString().equals(name) ? callableMemberDescriptor : null;
                }
            });

            if (overloadedFunctions.size() > 1) {
                Collections.sort(overloadedFunctions, OVERLOADED_MEMBER_COMPARATOR);
                counter = ContainerUtil.indexOfIdentity(overloadedFunctions, descriptor);
                assert counter >= 0;
            }
        }

        String name = descriptor.getName().asString();
        return counter == 0 ? name : name + '_' + counter;
    }

    private static String getArgumentTypesAsString(CallableDescriptor descriptor) {
        StringBuilder argTypes = new StringBuilder();

        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        if (receiverParameter != null) {
            argTypes.append(getJetTypeFqName(receiverParameter.getType(), true)).append(".");
        }

        argTypes.append(StringUtil.join(descriptor.getValueParameters(), new Function<ValueParameterDescriptor, String>() {
            @Override
            public String fun(ValueParameterDescriptor descriptor) {
                return getJetTypeFqName(descriptor.getType(), true);
            }
        }, ","));

        return argTypes.toString();
    }

    @NotNull
    public static String getStableMangledNameForDescriptor(@NotNull ClassDescriptor descriptor, @NotNull String functionName) {
        Collection<FunctionDescriptor> functions = descriptor.getDefaultType().getMemberScope().getFunctions(Name.identifier(functionName));
        assert functions.size() == 1 : "Can't select a single function: " + functionName + " in " + descriptor;
        return getSuggestedName(functions.iterator().next());
    }

    private static class OverloadedMemberComparator implements Comparator<CallableMemberDescriptor> {
        @Override
        public int compare(@NotNull CallableMemberDescriptor a, @NotNull CallableMemberDescriptor b) {
            // native functions first
            if (isNativeOrOverrideNative(a)) {
                if (!isNativeOrOverrideNative(b)) return -1;
            }
            else if (isNativeOrOverrideNative(b)) {
                return 1;
            }

            // be visibility
            // Actually "internal" > "private", but we want to have less number for "internal", so compare b with a instead of a with b.
            Integer result = Visibilities.compare(b.getVisibility(), a.getVisibility());
            if (result != null && result != 0) return result;

            // by arity
            int aArity = arity(a);
            int bArity = arity(b);
            if (aArity != bArity) return aArity - bArity;

            // by stringify argument types
            String aArguments = getArgumentTypesAsString(a);
            String bArguments = getArgumentTypesAsString(b);
            assert aArguments != bArguments;

            return aArguments.compareTo(bArguments);
        }

        private static int arity(CallableMemberDescriptor descriptor) {
            return descriptor.getValueParameters().size() + (descriptor.getExtensionReceiverParameter() == null ? 0 : 1);
        }

        private static boolean isNativeOrOverrideNative(CallableMemberDescriptor descriptor) {
            if (AnnotationsUtils.isNativeObject(descriptor)) return true;

            Set<CallableMemberDescriptor> declarations = OverrideResolver.getAllOverriddenDeclarations(descriptor);
            for (CallableMemberDescriptor memberDescriptor : declarations) {
                if (AnnotationsUtils.isNativeObject(memberDescriptor)) return true;
            }
            return false;
        }
    }
}
