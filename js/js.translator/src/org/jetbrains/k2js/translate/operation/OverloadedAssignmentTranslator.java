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

package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.CallBuilder;
import org.jetbrains.k2js.translate.utils.BindingUtils;

/**
 * @author Pavel Talanov
 */
public final class OverloadedAssignmentTranslator extends AssignmentTranslator {

    @NotNull
    public static JsExpression doTranslate(@NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context) {
        return (new OverloadedAssignmentTranslator(expression, context)).translate();
    }

    @NotNull
    private final FunctionDescriptor operationDescriptor;

    private OverloadedAssignmentTranslator(@NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context) {
        super(expression, context);
        FunctionDescriptor functionDescriptor =
                BindingUtils.getFunctionDescriptorForOperationExpression(context.bindingContext(), expression);
        assert functionDescriptor != null : "";
        this.operationDescriptor = functionDescriptor;
    }

    @NotNull
    private JsExpression translate() {
        if (isVariableReassignment) {
            return reassignment();
        }
        return overloadedMethodInvocation();
    }

    @NotNull
    private JsExpression reassignment() {
        return accessTranslator.translateAsSet(overloadedMethodInvocation());
    }

    @NotNull
    private JsExpression overloadedMethodInvocation() {
        return CallBuilder.build(context())
                .descriptor(operationDescriptor)
                .receiver(accessTranslator.translateAsGet())
                .args(right)
                .translate();
    }
}
