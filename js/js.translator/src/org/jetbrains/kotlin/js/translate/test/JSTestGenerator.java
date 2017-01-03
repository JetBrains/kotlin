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

package org.jetbrains.kotlin.js.translate.test;

import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsNew;
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.JetTestFunctionDetector;
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

//TODO: use method object instead of static functions
public final class JSTestGenerator {
    private JSTestGenerator() {
    }

    public static void generateTestCalls(@NotNull TranslationContext context,
            @NotNull Collection<KtFile> files, @NotNull JSTester tester) {
        List<FunctionDescriptor> functionDescriptors = JetTestFunctionDetector.getTestFunctionDescriptors(context.bindingContext(), files);
        doGenerateTestCalls(functionDescriptors, context, tester);
    }

    private static void doGenerateTestCalls(@NotNull List<FunctionDescriptor> functionDescriptors,
            @NotNull TranslationContext context, @NotNull JSTester jsTester) {
        for (FunctionDescriptor functionDescriptor : functionDescriptors) {
            ClassDescriptor classDescriptor = DescriptorUtils.getContainingClass(functionDescriptor);
            if (classDescriptor == null) {
                return;
            }
            generateCodeForTestMethod(context, functionDescriptor, classDescriptor, jsTester);
        }
    }

    private static void generateCodeForTestMethod(@NotNull TranslationContext context,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull ClassDescriptor classDescriptor, @NotNull JSTester tester) {
        JsExpression expression = ReferenceTranslator.translateAsValueReference(classDescriptor, context);
        JsNew testClass = new JsNew(expression);
        JsExpression functionToTestCall = CallTranslator.INSTANCE.buildCall(context, functionDescriptor,
                                                                             Collections.<JsExpression>emptyList(), testClass);
        JsStringLiteral testName = context.program().getStringLiteral(classDescriptor.getName() + "." + functionDescriptor.getName());
        tester.constructTestMethodInvocation(functionToTestCall, testName);
    }
}
