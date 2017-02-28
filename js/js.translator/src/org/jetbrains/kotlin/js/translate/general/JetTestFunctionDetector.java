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

package org.jetbrains.kotlin.js.translate.general;

import com.google.common.collect.Lists;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;
import java.util.List;

/**
 * Helps find functions which are annotated with a @Test annotation from junit
 */
public class JetTestFunctionDetector {
    private JetTestFunctionDetector() {
    }

    private static boolean isTest(@NotNull FunctionDescriptor functionDescriptor) {
        Annotations annotations = functionDescriptor.getAnnotations();
        for (AnnotationDescriptor annotation : annotations) {
            // TODO ideally we should find the fully qualified name here...
            KotlinType type = annotation.getType();
            String name = type.toString();
            if (name.equals("Test")) {
                return true;
            }
        }

        /*
        if (function.getName().startsWith("test")) {
            List<JetParameter> parameters = function.getValueParameters();
            return parameters.size() == 0;
        }
        */
        return false;
    }

    @NotNull
    public static List<FunctionDescriptor> getTestFunctionDescriptors(
            @NotNull ModuleDescriptor moduleDescriptor
    ) {
        List<FunctionDescriptor> answer = Lists.newArrayList();
        getTestFunctions(FqName.ROOT, moduleDescriptor, answer);
        return answer;
    }

    private static void getTestFunctions(
            @NotNull FqName packageName,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull List<FunctionDescriptor> foundFunctions
    ) {
        for (PackageFragmentDescriptor packageDescriptor : moduleDescriptor.getPackage(packageName).getFragments()) {
            if (DescriptorUtils.getContainingModule(packageDescriptor) != moduleDescriptor) continue;
            Collection<DeclarationDescriptor> descriptors = packageDescriptor.getMemberScope().getContributedDescriptors(
                    DescriptorKindFilter.CLASSIFIERS, MemberScope.Companion.getALL_NAME_FILTER());
            for (DeclarationDescriptor descriptor : descriptors) {
                if (descriptor instanceof ClassDescriptor) {
                    getTestFunctions((ClassDescriptor) descriptor, foundFunctions);
                }
            }
        }

        for (FqName subpackageName : moduleDescriptor.getSubPackagesOf(packageName, MemberScope.Companion.getALL_NAME_FILTER())) {
            getTestFunctions(subpackageName, moduleDescriptor, foundFunctions);
        }
    }

    private static void getTestFunctions(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull List<FunctionDescriptor> foundFunctions
    ) {
        if (classDescriptor.getModality() == Modality.ABSTRACT) return;


                Collection<DeclarationDescriptor> allDescriptors = classDescriptor.getUnsubstitutedMemberScope().getContributedDescriptors(
                DescriptorKindFilter.FUNCTIONS, MemberScope.Companion.getALL_NAME_FILTER());
                List<FunctionDescriptor> testFunctions = ContainerUtil.mapNotNull(
                        allDescriptors,
                         descriptor-> {
                                if (descriptor instanceof FunctionDescriptor) {
                                    FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                                    if (isTest(functionDescriptor)) return functionDescriptor;
                                }

                                return null;
                            });


                foundFunctions.addAll(testFunctions);

    }
}
