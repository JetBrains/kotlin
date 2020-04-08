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

package org.jetbrains.kotlin.jvm.compiler;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.jvm.JvmBindingContextSlices;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;

import java.util.*;

import static org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.assertNotNull;
import static org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.assertSameElements;

public class ExpectedLoadErrorsUtil {
    public static final String ANNOTATION_CLASS_NAME = "org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError";

    public static void checkForLoadErrors(
            @NotNull PackageViewDescriptor packageFromJava,
            @NotNull BindingContext bindingContext
    ) {
        Map<SourceElement, List<String>> expectedErrors = getExpectedLoadErrors(packageFromJava);
        Map<SourceElement, List<String>> actualErrors = getActualLoadErrors(bindingContext);

        for (SourceElement source : ContainerUtil.union(expectedErrors.keySet(), actualErrors.keySet())) {
            List<String> actual = actualErrors.get(source);
            List<String> expected = expectedErrors.get(source);

            assertNotNull("Unexpected load error(s):\n" + actual + "\ncontainer:" + source, expected);
            assertNotNull("Missing load error(s):\n" + expected + "\ncontainer:" + source, actual);

            assertSameElements("Unexpected/missing load error(s)\ncontainer:" + source, actual, expected);
        }
    }

    private static Map<SourceElement, List<String>> getExpectedLoadErrors(@NotNull PackageViewDescriptor packageFromJava) {
        Map<SourceElement, List<String>> map = new HashMap<>();

        packageFromJava.acceptVoid(new DeclarationDescriptorVisitorEmptyBodies<Void, Void>() {
            @Override
            public Void visitPackageViewDescriptor(PackageViewDescriptor descriptor, Void data) {
                return visitDeclarationRecursively(descriptor, descriptor.getMemberScope());
            }

            @Override
            public Void visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                return visitDeclarationRecursively(descriptor, descriptor.getDefaultType().getMemberScope());
            }

            @Override
            public Void visitFunctionDescriptor(FunctionDescriptor descriptor, Void data) {
                return visitDeclaration(descriptor);
            }

            @Override
            public Void visitPropertyDescriptor(PropertyDescriptor descriptor, Void data) {
                return visitDeclaration(descriptor);
            }

            private Void visitDeclaration(@NotNull DeclarationDescriptor descriptor) {
                AnnotationDescriptor annotation = descriptor.getAnnotations().findAnnotation(new FqName(ANNOTATION_CLASS_NAME));
                if (annotation == null) return null;

                // we expect exactly one annotation argument
                ConstantValue<?> argument = annotation.getAllValueArguments().values().iterator().next();

                String error = (String) argument.getValue();
                //noinspection ConstantConditions
                List<String> errors = Arrays.asList(error.split("\\|"));

                putError(map, descriptor, errors);

                return null;
            }

            private Void visitDeclarationRecursively(@NotNull DeclarationDescriptor descriptor, @NotNull MemberScope memberScope) {
                for (DeclarationDescriptor member : DescriptorUtils.getAllDescriptors(memberScope)) {
                    member.acceptVoid(this);
                }

                return visitDeclaration(descriptor);
            }
        });

        return map;
    }

    private static Map<SourceElement, List<String>> getActualLoadErrors(@NotNull BindingContext bindingContext) {
        Map<SourceElement, List<String>> result = new HashMap<>();

        Collection<DeclarationDescriptor> descriptors = bindingContext.getKeys(JvmBindingContextSlices.LOAD_FROM_JAVA_SIGNATURE_ERRORS);
        for (DeclarationDescriptor descriptor : descriptors) {
            List<String> errors = bindingContext.get(JvmBindingContextSlices.LOAD_FROM_JAVA_SIGNATURE_ERRORS, descriptor);
            if (errors == null) continue;

            putError(result, descriptor, errors);
        }

        return result;
    }

    private static void putError(
            @NotNull Map<SourceElement, List<String>> result,
            @NotNull DeclarationDescriptor descriptor,
            @NotNull List<String> errors
    ) {
        assert descriptor.getOriginal() instanceof DeclarationDescriptorWithSource
                : "Signature errors should be reported only on declarations with source, but " + descriptor + " found";
        result.put(((DeclarationDescriptorWithSource) descriptor.getOriginal()).getSource(), errors);
    }

    private ExpectedLoadErrorsUtil() {
    }
}
