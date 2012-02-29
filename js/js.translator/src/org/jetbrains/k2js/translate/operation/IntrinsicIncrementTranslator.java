/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.ReferenceAccessTranslator;

import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isPrefix;


/**
 * @author Pavel Talanov
 */
public final class IntrinsicIncrementTranslator extends IncrementTranslator {


    @NotNull
    public static JsExpression doTranslate(@NotNull JetUnaryExpression expression,
                                           @NotNull TranslationContext context) {
        return (new IntrinsicIncrementTranslator(expression, context))
                .translate();
    }

    private IntrinsicIncrementTranslator(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        super(expression, context);
    }

    @NotNull
    private JsExpression translate() {
        if (isPrimitiveExpressionIncrement()) {
            return jsUnaryExpression();
        }
        return translateAsMethodCall();
    }

    private boolean isPrimitiveExpressionIncrement() {
        return accessTranslator instanceof ReferenceAccessTranslator;
    }

    @NotNull
    private JsExpression jsUnaryExpression() {
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(getOperationToken(expression));
        JsExpression getExpression = accessTranslator.translateAsGet();
        if (isPrefix(expression)) {
            return new JsPrefixOperation(operator, getExpression);
        }
        else {
            return new JsPostfixOperation(operator, getExpression);
        }
    }

    @Override
    @NotNull
    protected JsExpression operationExpression(@NotNull JsExpression receiver) {
        return unaryAsBinary(receiver);
    }

    public JsBinaryOperation unaryAsBinary(@NotNull JsExpression leftExpression) {
        JsNumberLiteral oneLiteral = program().getNumberLiteral(1);
        JetToken token = getOperationToken(expression);
        if (token.equals(JetTokens.PLUSPLUS)) {
            return new JsBinaryOperation(JsBinaryOperator.ADD, leftExpression, oneLiteral);
        }
        if (token.equals(JetTokens.MINUSMINUS)) {
            return new JsBinaryOperation(JsBinaryOperator.SUB, leftExpression, oneLiteral);
        }
        throw new AssertionError("This method should be called only for increment and decrement operators");
    }

}
