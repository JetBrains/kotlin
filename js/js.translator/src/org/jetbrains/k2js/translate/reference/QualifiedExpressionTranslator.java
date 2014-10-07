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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.ErrorReportingUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import static org.jetbrains.k2js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.k2js.translate.reference.CallExpressionTranslator.shouldBeInlined;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getNotNullSimpleNameSelector;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getSelector;

public final class QualifiedExpressionTranslator {

    private QualifiedExpressionTranslator() {
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull JetQualifiedExpression expression,
                                                       @NotNull TranslationContext context, boolean forceOrderOfEvaluation) {
        JsExpression receiver = translateReceiver(expression, context);
        if (forceOrderOfEvaluation && receiver != null) {
            TemporaryVariable temporaryVariable = context.declareTemporary(null);
            context.addStatementToCurrentBlock(JsAstUtils.assignment(temporaryVariable.reference(), receiver).makeStmt());
            receiver = temporaryVariable.reference();
        }
        return VariableAccessTranslator.newInstance(context, getNotNullSimpleNameSelector(expression), receiver);
    }

    @NotNull
    public static JsExpression translateQualifiedExpression(@NotNull JetQualifiedExpression expression,
                                                            @NotNull TranslationContext context) {
        JsExpression receiver = translateReceiver(expression, context);
        JetExpression selector = getSelector(expression);
        return dispatchToCorrectTranslator(receiver, selector, context);
    }

    @NotNull
    private static JsExpression dispatchToCorrectTranslator(
            @Nullable JsExpression receiver,
            @NotNull JetExpression selector,
            @NotNull TranslationContext context
    ) {
        if (ReferenceTranslator.canBePropertyAccess(selector, context)) {
            assert selector instanceof JetSimpleNameExpression : "Selectors for properties must be simple names.";
            return VariableAccessTranslator.newInstance(context, (JetSimpleNameExpression)selector, receiver).translateAsGet();
        }
        if (selector instanceof JetCallExpression) {
            if (shouldBeInlined((JetCallExpression) selector, context) &&
                BindingContextUtilPackage.isUsedAsExpression(selector, context.bindingContext())) {
                TemporaryVariable temporaryVariable = context.declareTemporary(null);
                JsExpression result = invokeCallExpressionTranslator(receiver, selector, context);
                context.addStatementToCurrentBlock(JsAstUtils.assignment(temporaryVariable.reference(), result).makeStmt());
                return temporaryVariable.reference();
            } else {
                return invokeCallExpressionTranslator(receiver, selector, context);
            }
        }
        //TODO: never get there
        if (selector instanceof JetSimpleNameExpression) {
            return ReferenceTranslator.translateSimpleNameWithQualifier((JetSimpleNameExpression) selector, receiver, context);
        }
        throw new AssertionError("Unexpected qualified expression: " + selector.getText());
    }

    @NotNull
    private static JsExpression invokeCallExpressionTranslator(
            @Nullable JsExpression receiver,
            @NotNull JetExpression selector,
            @NotNull TranslationContext context
    ) {
        try {
            return CallExpressionTranslator.translate((JetCallExpression) selector, receiver, context);
        } catch (RuntimeException e) {
            throw  ErrorReportingUtils.reportErrorWithLocation(selector, e);
        }
    }

    @Nullable
    private static JsExpression translateReceiver(@NotNull JetQualifiedExpression expression,
                                                  @NotNull TranslationContext context) {
        JetExpression receiverExpression = expression.getReceiverExpression();
        if (isFullQualifierForExpression(receiverExpression, context)) {
            return null;
        }
        return translateAsExpression(receiverExpression, context);
    }

    //TODO: prove correctness
    private static boolean isFullQualifierForExpression(@Nullable JetExpression receiverExpression, @NotNull TranslationContext context) {
        if (receiverExpression == null) {
            return false;
        }
        if (receiverExpression instanceof JetReferenceExpression) {
            DeclarationDescriptor descriptorForReferenceExpression =
                getDescriptorForReferenceExpression(context.bindingContext(), (JetReferenceExpression)receiverExpression);
            if (descriptorForReferenceExpression instanceof PackageViewDescriptor) {
                return true;
            }
        }
        if (receiverExpression instanceof JetQualifiedExpression) {
            return isFullQualifierForExpression(((JetQualifiedExpression)receiverExpression).getSelectorExpression(), context);
        }
        return false;
    }
}
