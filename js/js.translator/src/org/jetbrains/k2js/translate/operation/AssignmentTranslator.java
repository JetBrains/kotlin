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

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.reference.AccessTranslationUtils;
import org.jetbrains.k2js.translate.reference.AccessTranslator;

import static org.jetbrains.k2js.translate.utils.BindingUtils.isVariableReassignment;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isAssignment;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.hasCorrespondingFunctionIntrinsic;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateRightExpression;

public abstract class AssignmentTranslator extends AbstractTranslator {

    public static boolean isAssignmentOperator(JetBinaryExpression expression) {
        JetToken operationToken = getOperationToken(expression);
        return (OperatorConventions.ASSIGNMENT_OPERATIONS.keySet().contains(operationToken)
                || isAssignment(operationToken));
    }

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        if (hasCorrespondingFunctionIntrinsic(context, expression)) {
            return IntrinsicAssignmentTranslator.doTranslate(expression, context);
        }
        return OverloadedAssignmentTranslator.doTranslate(expression, context);
    }

    @NotNull
    protected final JetBinaryExpression expression;
    protected final AccessTranslator accessTranslator;
    protected final boolean isVariableReassignment;
    @NotNull
    protected final JsExpression right;

    protected AssignmentTranslator(@NotNull JetBinaryExpression expression,
                                   @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.isVariableReassignment = isVariableReassignment(context.bindingContext(), expression);
        JetExpression left = expression.getLeft();
        assert left != null : "No left-hand side: " + expression.getText();
        this.accessTranslator = AccessTranslationUtils.getAccessTranslator(left, context());
        this.right = translateRightExpression(context(), expression);
    }
}
