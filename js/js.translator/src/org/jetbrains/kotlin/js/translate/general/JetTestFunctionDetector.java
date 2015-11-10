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
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.Modality;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
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
            @NotNull BindingContext bindingContext,
            @NotNull Collection<KtFile> files
    ) {
        List<FunctionDescriptor> answer = Lists.newArrayList();
        for (KtFile file : files) {
            answer.addAll(getTestFunctions(bindingContext, file.getDeclarations()));
        }
        return answer;
    }

    @NotNull
    private static List<FunctionDescriptor> getTestFunctions(
            @NotNull BindingContext bindingContext,
            @NotNull List<KtDeclaration> declarations
    ) {
        List<FunctionDescriptor> answer = Lists.newArrayList();
        for (KtDeclaration declaration : declarations) {
            MemberScope scope = null;

            if (declaration instanceof KtClass) {
                KtClass klass = (KtClass) declaration;
                ClassDescriptor classDescriptor = BindingUtils.getClassDescriptor(bindingContext, klass);

                if (classDescriptor.getModality() != Modality.ABSTRACT) {
                    scope = classDescriptor.getDefaultType().getMemberScope();
                }
            }

            if (scope != null) {
                Collection<DeclarationDescriptor> allDescriptors = DescriptorUtils.getAllDescriptors(scope);
                List<FunctionDescriptor> testFunctions = ContainerUtil.mapNotNull(
                        allDescriptors,
                        new Function<DeclarationDescriptor, FunctionDescriptor>() {
                            @Override
                            public FunctionDescriptor fun(DeclarationDescriptor descriptor) {
                                if (descriptor instanceof FunctionDescriptor) {
                                    FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                                    if (isTest(functionDescriptor)) return functionDescriptor;
                                }

                                return null;
                            }
                        });

                answer.addAll(testFunctions);
            }
        }
        return answer;
    }
}
