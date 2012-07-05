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

package org.jetbrains.k2js.translate.test;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.JetTestFunctionDetector;
import org.jetbrains.k2js.translate.reference.CallBuilder;
import org.jetbrains.k2js.translate.reference.ReferenceTranslator;
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils;

import java.util.Collection;
import java.util.List;

import static com.google.dart.compiler.util.AstUtil.newBlock;

/**
 * @author Pavel Talanov
 */
public final class TestGenerator {
    private TestGenerator() {
    }

    public static void generateTestCalls(@NotNull TranslationContext context,
            @NotNull Collection<JetFile> files,
            @NotNull JsBlock block) {
        List<FunctionDescriptor> functionDescriptors = JetTestFunctionDetector.getTestFunctionDescriptors(context.bindingContext(), files);
        doGenerateTestCalls(functionDescriptors, block, context);
    }

    private static void doGenerateTestCalls(@NotNull List<FunctionDescriptor> functionDescriptors,
            @NotNull JsBlock block,
            @NotNull TranslationContext context) {
        for (FunctionDescriptor functionDescriptor : functionDescriptors) {
            ClassDescriptor classDescriptor = JsDescriptorUtils.getContainingClass(functionDescriptor);
            if (classDescriptor == null) {
                return;
            }
            JsExpression expression = ReferenceTranslator.translateAsFQReference(classDescriptor, context);
            JsNew constructClassExpr = new JsNew(expression);
            JsExpression functionToTestCall =
                    CallBuilder.build(context).descriptor(functionDescriptor).receiver(constructClassExpr).translate();
            JsNameRef qUnitTestFunRef = AstUtil.newQualifiedNameRef("QUnit.test");
            JsFunction functionToTest = new JsFunction(context.jsScope());
            functionToTest.setBody(newBlock(functionToTestCall.makeStmt()));
            String testName = classDescriptor.getName() + "." + functionDescriptor.getName();
            block.getStatements()
                    .add(AstUtil.newInvocation(qUnitTestFunRef, context.program().getStringLiteral(testName), functionToTest)
                                 .makeStmt());
        }
    }
}
