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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.reference.AccessTranslationUtils;
import org.jetbrains.kotlin.js.translate.reference.AccessTranslator;
import org.jetbrains.kotlin.js.translate.reference.BackingFieldAccessTranslator;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.isVariableReassignment;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getSimpleName;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.isAssignment;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.hasCorrespondingFunctionIntrinsic;

public abstract class AssignmentTranslator extends AbstractTranslator {

    public static boolean isAssignmentOperator(KtToken operationToken) {
        return (OperatorConventions.ASSIGNMENT_OPERATIONS.keySet().contains(operationToken) || isAssignment(operationToken));
    }

    @NotNull
    public static JsExpression translate(@NotNull KtBinaryExpression expression, @NotNull TranslationContext context) {
        if (hasCorrespondingFunctionIntrinsic(context, expression)) {
            return IntrinsicAssignmentTranslator.doTranslate(expression, context);
        }
        return OverloadedAssignmentTranslator.doTranslate(expression, context);
    }

    @NotNull
    protected final KtBinaryExpression expression;
    protected final boolean isVariableReassignment;

    protected AssignmentTranslator(@NotNull KtBinaryExpression expression, @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.isVariableReassignment = isVariableReassignment(context.bindingContext(), expression);
        assert expression.getLeft() != null : "No left-hand side: " + expression.getText();
    }

    protected final AccessTranslator createAccessTranslator(@NotNull KtExpression left, boolean forceOrderOfEvaluation) {
        if (isReferenceToBackingFieldFromConstructor(left, context())) {
            KtSimpleNameExpression simpleName = getSimpleName(left);
            assert simpleName != null;
            return BackingFieldAccessTranslator.newInstance(simpleName, context());
        }
        else {
            return AccessTranslationUtils.getAccessTranslator(left, context(), forceOrderOfEvaluation);
        }
    }

    private static boolean isReferenceToBackingFieldFromConstructor(
            @NotNull KtExpression expression,
            @NotNull TranslationContext context
    ) {
        if (expression instanceof KtSimpleNameExpression) {
            KtSimpleNameExpression nameExpression = (KtSimpleNameExpression) expression;
            DeclarationDescriptor descriptor = getDescriptorForReferenceExpression(context.bindingContext(), nameExpression);
            return isReferenceToBackingFieldFromConstructor(descriptor, context);
        }
        else if (expression instanceof KtDotQualifiedExpression) {
            KtDotQualifiedExpression qualifiedExpression = (KtDotQualifiedExpression) expression;
            if (qualifiedExpression.getReceiverExpression() instanceof KtThisExpression &&
                qualifiedExpression.getSelectorExpression() instanceof KtSimpleNameExpression
            ) {
                KtSimpleNameExpression nameExpression = (KtSimpleNameExpression) qualifiedExpression.getSelectorExpression();
                DeclarationDescriptor descriptor = getDescriptorForReferenceExpression(context.bindingContext(), nameExpression);
                return isReferenceToBackingFieldFromConstructor(descriptor, context);
            }
        }
        return false;
    }

    private static boolean isReferenceToBackingFieldFromConstructor(
            @Nullable DeclarationDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        if (!(descriptor instanceof PropertyDescriptor)) return false;

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
        if (!(context.getDeclarationDescriptor() instanceof ClassDescriptor)) return false;

        ClassDescriptor classDescriptor = (ClassDescriptor) context.getDeclarationDescriptor();
        if (classDescriptor != propertyDescriptor.getContainingDeclaration()) return false;

        return !propertyDescriptor.isVar();
    }
}
