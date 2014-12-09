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

package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.isCompareTo;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;

public final class CompareToTranslator extends AbstractTranslator {

    public static boolean isCompareToCall(
            @NotNull JetToken operationToken,
            @Nullable CallableDescriptor operationDescriptor
    ) {
        if (!OperatorConventions.COMPARISON_OPERATIONS.contains(operationToken) || operationDescriptor == null) return false;

        return isCompareTo(operationDescriptor);
    }

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context) {
        return (new CompareToTranslator(expression, context)).translate();
    }

    @NotNull
    private final JetBinaryExpression expression;

    private CompareToTranslator(
            @NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context
    ) {
        super(context);
        this.expression = expression;
        CallableDescriptor descriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression);
        assert descriptor != null : "CompareTo should always have a descriptor";
        assert (OperatorConventions.COMPARISON_OPERATIONS.contains(getOperationToken(expression))) :
                message(expression, "CompareToTranslator supported only expressions with operation token from COMPARISON_OPERATIONS, " +
                                    "expression: " + expression.getText());
    }

    @NotNull
    private JsExpression translate() {
        JsBinaryOperator correspondingOperator = OperatorTable.getBinaryOperator(getOperationToken(expression));
        JsExpression methodCall = BinaryOperationTranslator.translateAsOverloadedCall(expression, context());
        return new JsBinaryOperation(correspondingOperator, methodCall, context().program().getNumberLiteral(0));
    }
}
