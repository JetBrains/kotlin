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

package org.jetbrains.kotlin.js.translate.operation;

import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtUnaryExpression;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;

import static org.jetbrains.kotlin.js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCompileTimeValue;
import static org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getBaseExpression;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.*;

public final class UnaryOperationTranslator {
    private UnaryOperationTranslator() {
    }

    @NotNull
    public static JsExpression translate(
            @NotNull KtUnaryExpression expression,
            @NotNull TranslationContext context
    ) {
        IElementType operationToken = expression.getOperationReference().getReferencedNameElementType();
        if (operationToken == KtTokens.EXCLEXCL) {
            KtExpression baseExpression = getBaseExpression(expression);
            JsExpression translatedExpression = translateAsExpression(baseExpression, context);
            return sure(translatedExpression, context);
        }

        if (operationToken == KtTokens.MINUS) {
            KtExpression baseExpression = getBaseExpression(expression);
            if (baseExpression instanceof KtConstantExpression) {
                CompileTimeConstant<?> compileTimeValue = ConstantExpressionEvaluator.getConstant(expression, context.bindingContext());
                assert compileTimeValue != null : message(expression, "Expression is not compile time value: " + expression.getText() + " ");
                Object value = getCompileTimeValue(context.bindingContext(), expression, compileTimeValue);
                if (value instanceof Long) {
                    return JsAstUtils.newLong((Long) value, context);
                }
            }
        }

        if (IncrementTranslator.isIncrement(operationToken)) {
            return IncrementTranslator.translate(expression, context);
        }

        JsExpression baseExpression = TranslationUtils.translateBaseExpression(context, expression);
        if (isExclForBinaryEqualLikeExpr(expression, baseExpression)) {
            return translateExclForBinaryEqualLikeExpr((JsBinaryOperation) baseExpression);
        }

        ResolvedCall<? extends FunctionDescriptor> resolvedCall = CallUtilKt.getFunctionResolvedCallWithAssert(expression, context.bindingContext());
        return CallTranslator.translate(context, resolvedCall, baseExpression);
    }

    private static boolean isExclForBinaryEqualLikeExpr(@NotNull KtUnaryExpression expression, @NotNull JsExpression baseExpression) {
        if (getOperationToken(expression).equals(KtTokens.EXCL)) {
            if (baseExpression instanceof JsBinaryOperation) {
                return isEqualLikeOperator(((JsBinaryOperation) baseExpression).getOperator());
            }
        }
        return false;
    }
}
