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

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.JetConstantExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetUnaryExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.types.JetType;

import static org.jetbrains.kotlin.js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCompileTimeValue;
import static org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getBaseExpression;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.*;
import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getFunctionResolvedCallWithAssert;

public final class UnaryOperationTranslator {
    private UnaryOperationTranslator() {
    }

    @NotNull
    public static JsExpression translate(
            @NotNull JetUnaryExpression expression,
            @NotNull TranslationContext context
    ) {
        IElementType operationToken = expression.getOperationReference().getReferencedNameElementType();
        if (operationToken == JetTokens.EXCLEXCL) {
            JetExpression baseExpression = getBaseExpression(expression);
            JetType type = BindingContextUtils.getNotNull(context.bindingContext(), BindingContext.EXPRESSION_TYPE, baseExpression);
            JsExpression translatedExpression = translateAsExpression(baseExpression, context);
            return type.isMarkedNullable() ? sure(translatedExpression, context) : translatedExpression;
        }

        if (operationToken == JetTokens.MINUS) {
            JetExpression baseExpression = getBaseExpression(expression);
            if (baseExpression instanceof JetConstantExpression) {
                CompileTimeConstant<?> compileTimeValue = context.bindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
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

        ResolvedCall<? extends FunctionDescriptor> resolvedCall = getFunctionResolvedCallWithAssert(expression, context.bindingContext());
        return CallTranslator.INSTANCE$.translate(context, resolvedCall, baseExpression);
    }

    private static boolean isExclForBinaryEqualLikeExpr(@NotNull JetUnaryExpression expression, @NotNull JsExpression baseExpression) {
        if (getOperationToken(expression).equals(JetTokens.EXCL)) {
            if (baseExpression instanceof JsBinaryOperation) {
                return isEqualLikeOperator(((JsBinaryOperation) baseExpression).getOperator());
            }
        }
        return false;
    }
}
