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

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsConditional;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.CallBuilder;
import org.jetbrains.k2js.translate.reference.CallType;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.Collections;

import static org.jetbrains.k2js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCall;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getBaseExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.notNullCheck;

/**
 * @author Pavel Talanov
 */

public final class UnaryOperationTranslator {

    private UnaryOperationTranslator() {
    }

    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        if (isExclExcl(expression)) {
            return translateExclExclOperator(expression, context);
        }
        if (IncrementTranslator.isIncrement(expression)) {
            return IncrementTranslator.translate(expression, context);
        }
        return translateAsCall(expression, context);
    }

    private static boolean isExclExcl(@NotNull JetUnaryExpression expression) {
        return getOperationToken(expression).equals(JetTokens.EXCLEXCL);
    }

    @NotNull
    private static JsExpression translateExclExclOperator(@NotNull JetUnaryExpression expression, @NotNull TranslationContext context) {
        JsNameRef cachedValue = context.declareTemporary(translateAsExpression(getBaseExpression(expression), context), true).reference();
        JsBinaryOperation notNullCheck = notNullCheck(context, cachedValue);
        return new JsConditional(notNullCheck, cachedValue, context.namer().throwNPEFunctionCall());
    }

    @NotNull
    private static JsExpression translateAsCall(@NotNull JetUnaryExpression expression,
                                                @NotNull TranslationContext context) {
        return CallBuilder.build(context)
                .receiver(TranslationUtils.translateBaseExpression(context, expression))
                .args(Collections.<JsExpression>emptyList())
                .resolvedCall(getResolvedCall(context.bindingContext(), expression.getOperationReference()))
                .type(CallType.NORMAL).translate();
    }
}
