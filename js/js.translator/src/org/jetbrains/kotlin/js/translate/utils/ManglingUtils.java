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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.LookupLocation;

import java.util.*;

import static org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsPackage.getJetTypeFqName;
import static org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsPackage.hasPrimaryConstructor;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName;

public class ManglingUtils {
    private ManglingUtils() {}

    public static final Comparator<CallableDescriptor> CALLABLE_COMPARATOR = new CallableComparator();

    @NotNull
    public static String getMangledName(@NotNull PropertyDescriptor descriptor, @NotNull String suggestedName) {
        return getStableMangledName(suggestedName, getFqName(descriptor).asString());
    }

    @NotNull
    public static String getSuggestedName(@NotNull DeclarationDescriptor descriptor) {
        String suggestedName = descriptor.getName().asString();

        if (descriptor instanceof FunctionDescriptor ||
            descriptor instanceof PropertyDescriptor && DescriptorUtils.isExtension((PropertyDescriptor) descriptor)
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
        if (DescriptorUtils.isOverride(descriptor)) {
            return true;
        }

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();

        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            return descriptor.getVisibility().getIsPublicAPI();
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
            if (!classDescriptor.getVisibility().getIsPublicAPI()) {
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
    public static String getMangledMemberNameForExplicitDelegation(
            @NotNull String suggestedName,
            @NotNull FqNameUnsafe classFqName,
            @NotNull FqNameUnsafe typeFqName
    ) {
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
        String suggestedName = getSuggestedName(descriptor);
        return getStableMangledName(suggestedName, getArgumentTypesAsString(descriptor));
    }

    @NotNull
    private static String getSuggestedName(@NotNull CallableDescriptor descriptor) {
        if (descriptor instanceof ConstructorDescriptor && !((ConstructorDescriptor) descriptor).isPrimary()) {
            DeclarationDescriptor classDescriptor = descriptor.getContainingDeclaration();
            assert classDescriptor != null;
            return classDescriptor.getName().asString();
        }
        else {
            return descriptor.getName().asString();
        }
    }

    @NotNull
    private static String getSimpleMangledName(@NotNull CallableMemberDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();

        JetScope jetScope = null;

        String nameToCompare = descriptor.getName().asString();

        if (containingDeclaration != null && descriptor instanceof ConstructorDescriptor) {
            nameToCompare = containingDeclaration.getName().asString();
            containingDeclaration = containingDeclaration.getContainingDeclaration();
        }

        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            jetScope = ((PackageFragmentDescriptor) containingDeclaration).getMemberScope();
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            jetScope = ((ClassDescriptor) containingDeclaration).getDefaultType().getMemberScope();
        }

        int counter = 0;

        if (jetScope != null) {
            final String finalNameToCompare = nameToCompare;

            Collection<DeclarationDescriptor> declarations = jetScope.getDescriptors(DescriptorKindFilter.CALLABLES, JetScope.ALL_NAME_FILTER);
            List<CallableDescriptor> overloadedFunctions =
                    KotlinPackage.flatMap(declarations, new Function1<DeclarationDescriptor, Iterable<? extends CallableDescriptor>>() {
                @Override
                public Iterable<? extends CallableDescriptor> invoke(DeclarationDescriptor declarationDescriptor) {
                    if (declarationDescriptor instanceof ClassDescriptor && finalNameToCompare.equals(declarationDescriptor.getName().asString())) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                        Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();

                        if (!hasPrimaryConstructor(classDescriptor)) {
                            ConstructorDescriptorImpl fakePrimaryConstructor =
                                    ConstructorDescriptorImpl.create(classDescriptor, Annotations.EMPTY, true, SourceElement.NO_SOURCE);
                            return KotlinPackage.plus(constructors, fakePrimaryConstructor);
                        }

                        return constructors;
                    }

                    if (!(declarationDescriptor instanceof CallableMemberDescriptor)) return Collections.emptyList();

                    CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) declarationDescriptor;

                    String name = AnnotationsUtils.getNameForAnnotatedObjectWithOverrides(callableMemberDescriptor);

                    // when name == null it's mean that it's not native.
                    if (name == null) {
                        // skip functions without arguments, because we don't use mangling for them
                        if (needsStableMangling(callableMemberDescriptor) && !callableMemberDescriptor.getValueParameters().isEmpty()) return Collections.emptyList();

                        // TODO add prefix for property: get_$name and set_$name
                        name = callableMemberDescriptor.getName().asString();
                    }

                    if (finalNameToCompare.equals(name)) return Collections.singletonList(callableMemberDescriptor);

                    return Collections.emptyList();
                }
            });

            if (overloadedFunctions.size() > 1) {
                Collections.sort(overloadedFunctions, CALLABLE_COMPARATOR);
                counter = ContainerUtil.indexOfIdentity(overloadedFunctions, descriptor);
                assert counter >= 0;
            }
        }

        String name = getSuggestedName(descriptor);
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
        Collection<FunctionDescriptor> functions =
                descriptor.getDefaultType().getMemberScope().getFunctions(Name.identifier(functionName), LookupLocation.NO_LOCATION);
        assert functions.size() == 1 : "Can't select a single function: " + functionName + " in " + descriptor;
        return getSuggestedName((DeclarationDescriptor) functions.iterator().next());
    }

    private static class CallableComparator implements Comparator<CallableDescriptor> {
        @Override
        public int compare(@NotNull CallableDescriptor a, @NotNull CallableDescriptor b) {
            // primary constructors
            if (a instanceof ConstructorDescriptor && ((ConstructorDescriptor) a).isPrimary()) {
                if (!(b instanceof ConstructorDescriptor) || !((ConstructorDescriptor) b).isPrimary()) return -1;
            }
            else if (b instanceof ConstructorDescriptor && ((ConstructorDescriptor) b).isPrimary()) {
                return 1;
            }

            // native functions
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

        private static int arity(CallableDescriptor descriptor) {
            return descriptor.getValueParameters().size() + (descriptor.getExtensionReceiverParameter() == null ? 0 : 1);
        }

        private static boolean isNativeOrOverrideNative(CallableDescriptor descriptor) {
            if (!(descriptor instanceof CallableMemberDescriptor)) return false;

            if (AnnotationsUtils.isNativeObject(descriptor)) return true;

            Set<CallableMemberDescriptor> declarations = DescriptorUtils.getAllOverriddenDeclarations((CallableMemberDescriptor) descriptor);
            for (CallableMemberDescriptor memberDescriptor : declarations) {
                if (AnnotationsUtils.isNativeObject(memberDescriptor)) return true;
            }
            return false;
        }
    }
}
