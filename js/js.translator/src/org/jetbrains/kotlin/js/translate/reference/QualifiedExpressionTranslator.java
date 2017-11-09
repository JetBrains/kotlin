/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.reference;

import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;

import static org.jetbrains.kotlin.js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getNotNullSimpleNameSelector;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getSelector;

public final class QualifiedExpressionTranslator {

    private QualifiedExpressionTranslator() {
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull KtQualifiedExpression expression,
                                                       @NotNull TranslationContext context, boolean forceOrderOfEvaluation) {
        JsExpression receiver = translateReceiver(expression, context);
        if (forceOrderOfEvaluation && receiver != null) {
            receiver = context.defineTemporary(receiver);
        }
        return VariableAccessTranslator.newInstance(context, getNotNullSimpleNameSelector(expression), receiver);
    }

    @NotNull
    public static JsNode translateQualifiedExpression(
            @NotNull KtQualifiedExpression expression,
            @NotNull TranslationContext context
    ) {
        ResolvedCall<?> call = CallUtilKt.getResolvedCall(expression, context.bindingContext());
        JsExpression receiver = null;
        if (call != null) {
            receiver = translateReceiver(expression, context);
        }
        KtExpression selector = getSelector(expression);
        return dispatchToCorrectTranslator(receiver, selector, context);
    }

    @NotNull
    private static JsNode dispatchToCorrectTranslator(
            @Nullable JsExpression receiver,
            @NotNull KtExpression selector,
            @NotNull TranslationContext context
    ) {
        if (ReferenceTranslator.canBePropertyAccess(selector, context)) {
            assert selector instanceof KtSimpleNameExpression : "Selectors for properties must be simple names.";
            return VariableAccessTranslator.newInstance(context, (KtSimpleNameExpression)selector, receiver).translateAsGet();
        }
        if (selector instanceof KtCallExpression) {
            return invokeCallExpressionTranslator(receiver, selector, context);
        }
        //TODO: never get there
        if (selector instanceof KtSimpleNameExpression) {
            return ReferenceTranslator.translateSimpleName((KtSimpleNameExpression) selector, context);
        }
        throw new AssertionError("Unexpected qualified expression: " + selector.getText());
    }

    @NotNull
    private static JsNode invokeCallExpressionTranslator(
            @Nullable JsExpression receiver,
            @NotNull KtExpression selector,
            @NotNull TranslationContext context
    ) {
        try {
            return CallExpressionTranslator.translate((KtCallExpression) selector, receiver, context);
        } catch (RuntimeException e) {
            throw  ErrorReportingUtils.reportErrorWithLocation(selector, e);
        }
    }

    @Nullable
    private static JsExpression translateReceiver(@NotNull KtQualifiedExpression expression,
                                                  @NotNull TranslationContext context) {
        KtExpression receiverExpression = expression.getReceiverExpression();
        if (isFullQualifierForExpression(receiverExpression, context)) {
            return null;
        }
        return translateAsExpression(receiverExpression, context);
    }

    //TODO: prove correctness
    private static boolean isFullQualifierForExpression(@Nullable KtExpression receiverExpression, @NotNull TranslationContext context) {
        if (receiverExpression == null) {
            return false;
        }
        if (receiverExpression instanceof KtReferenceExpression) {
            DeclarationDescriptor descriptorForReferenceExpression =
                getDescriptorForReferenceExpression(context.bindingContext(), (KtReferenceExpression)receiverExpression);
            if (descriptorForReferenceExpression instanceof PackageViewDescriptor) {
                return true;
            }
        }
        if (receiverExpression instanceof KtQualifiedExpression) {
            return isFullQualifierForExpression(((KtQualifiedExpression)receiverExpression).getSelectorExpression(), context);
        }
        return false;
    }
}
