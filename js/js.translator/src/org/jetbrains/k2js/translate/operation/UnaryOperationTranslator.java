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
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.CallBuilder;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import static org.jetbrains.k2js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCall;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getBaseExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.isEqualLikeOperator;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.sure;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateExclForBinaryEqualLikeExpr;

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
            return sure(translateAsExpression(getBaseExpression(expression), context), context);
        }
        if (IncrementTranslator.isIncrement(operationToken)) {
            return IncrementTranslator.translate(expression, context);
        }

        JsExpression baseExpression = TranslationUtils.translateBaseExpression(context, expression);
        if (isExclForBinaryEqualLikeExpr(expression, baseExpression)) {
            return translateExclForBinaryEqualLikeExpr((JsBinaryOperation) baseExpression);
        }

        return CallBuilder.build(context)
                .receiver(TranslationUtils.translateBaseExpression(context, expression))
                .resolvedCall(getResolvedCall(context.bindingContext(), expression.getOperationReference()))
                .translate();
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
