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

package org.jetbrains.k2js.translate.intrinsic.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.factories.TopLevelFIF;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils;

import java.util.Arrays;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;

/**
 * @author Pavel Talanov
 */
public final class EqualsIntrinsic implements BinaryOperationIntrinsic {

    @Override
    public boolean isApplicable(@NotNull JetBinaryExpression expression, @NotNull TranslationContext context) {
        if (!OperatorConventions.EQUALS_OPERATIONS.contains(getOperationToken(expression))) {
            return false;
        }
        FunctionDescriptor functionDescriptor = getFunctionDescriptorForOperationExpression(context.bindingContext(), expression);
        assert functionDescriptor != null;
        return JsDescriptorUtils.isStandardDeclaration(functionDescriptor);
    }

    @Override
    @NotNull
    public JsExpression apply(@NotNull JetBinaryExpression expression,
            @NotNull JsExpression left,
            @NotNull JsExpression right,
            @NotNull TranslationContext context) {
        boolean isNegated = getOperationToken(expression).equals(JetTokens.EXCLEQ);
        JsExpression result = TopLevelFIF.EQUALS.apply(left, Arrays.asList(right), context);
        return isNegated ? JsAstUtils.negated(result) : result;
    }
}
