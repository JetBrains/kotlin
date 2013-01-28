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

package org.jetbrains.k2js.translate.general;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getNullableDescriptorForFunction;

/**
 * Helps find functions which are annotated with a @Test annotation from junit
 */
public class JetTestFunctionDetector {
    private JetTestFunctionDetector() {
    }

    private static boolean isTest(@NotNull BindingContext bindingContext, @NotNull JetNamedFunction function) {
        FunctionDescriptor functionDescriptor = getNullableDescriptorForFunction(bindingContext, function);
        if (functionDescriptor == null) {
            return false;
        }
        List<AnnotationDescriptor> annotations = functionDescriptor.getAnnotations();
        if (annotations != null) {
            for (AnnotationDescriptor annotation : annotations) {
                // TODO ideally we should find the fully qualified name here...
                JetType type = annotation.getType();
                String name = type.toString();
                if (name.equals("Test")) {
                    return true;
                }
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
    private static List<JetNamedFunction> findTestFunctions(@NotNull BindingContext bindingContext, @NotNull Collection<JetFile> files) {
        List<JetNamedFunction> answer = Lists.newArrayList();
        for (JetFile file : files) {
            answer.addAll(getTestFunctions(bindingContext, file.getDeclarations()));
        }
        return answer;
    }

    @NotNull
    public static List<FunctionDescriptor> getTestFunctionDescriptors(@NotNull BindingContext bindingContext, @NotNull Collection<JetFile> files) {
        List<FunctionDescriptor> answer = Lists.newArrayList();
        for (JetNamedFunction function : findTestFunctions(bindingContext, files)) {
            answer.add(getFunctionDescriptor(bindingContext, function));
        }
        return answer;
    }

    @NotNull
    private static List<JetNamedFunction> getTestFunctions(@NotNull BindingContext bindingContext,
            @NotNull List<JetDeclaration> declarations) {
        List<JetNamedFunction> answer = Lists.newArrayList();
        for (JetDeclaration declaration : declarations) {
            if (declaration instanceof JetClass) {
                JetClass klass = (JetClass) declaration;
                answer.addAll(getTestFunctions(bindingContext, klass.getDeclarations()));
            }
            else if (declaration instanceof JetNamedFunction) {
                JetNamedFunction candidateFunction = (JetNamedFunction) declaration;
                if (isTest(bindingContext, candidateFunction)) {
                    answer.add(candidateFunction);
                }
            }
        }
        return answer;
    }
}
