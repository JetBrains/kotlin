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

package org.jetbrains.kotlin.js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.patterns.DescriptorPredicate;
import org.jetbrains.kotlin.js.patterns.PatternBuilder;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;

/**
 * Translates 'A in B' expression applying specialization if possible
 */
public class InOperationTranslator extends AbstractTranslator {
    private static final DescriptorPredicate INT_SPECIALIZATION_TEST = PatternBuilder.pattern("ranges.IntRange.contains");
    private static final DescriptorPredicate INT_RANGE_TEST = PatternBuilder.pattern("Int.rangeTo");
    private final JsExpression left;
    private final KtExpression right;
    private final KtSimpleNameExpression operation;

    public InOperationTranslator(@NotNull TranslationContext context, @NotNull JsExpression left, @NotNull KtExpression right,
            @NotNull KtSimpleNameExpression operation) {
        super(context);
        this.left = left;
        this.right = right;
        this.operation = operation;
    }

    @NotNull
    public JsExpression translate() {
        ResolvedCall<? extends FunctionDescriptor> call = getResolvedFunctionCall(operation);
        if (INT_SPECIALIZATION_TEST.apply(call.getResultingDescriptor())) {
            JsExpression candidate = translateInt();
            if (candidate != null) {
                return candidate;
            }
        }
        JsExpression rightTranslated = Translation.translateAsExpression(right, context());
        return translateGeneral(call, rightTranslated);
    }

    @NotNull
    private ResolvedCall<? extends FunctionDescriptor> getResolvedFunctionCall(@NotNull KtExpression expression) {
        ResolvedCall<? extends CallableDescriptor> call = CallUtilKt.getResolvedCallWithAssert(expression, context().bindingContext());
        assert call.getResultingDescriptor() instanceof FunctionDescriptor : "A.rangeTo(B) must imply FunctionDescriptor: " +
                                                                             PsiUtilsKt.getTextWithLocation(operation);
        @SuppressWarnings("unchecked")
        ResolvedCall<? extends FunctionDescriptor> functionCall = (ResolvedCall<? extends FunctionDescriptor>) call;
        return functionCall;
    }

    @NotNull
    private JsExpression translateGeneral(@NotNull ResolvedCall<? extends FunctionDescriptor> call, @NotNull JsExpression rightTranslated) {
        return CallTranslator.translate(context(), call, rightTranslated);
    }

    @Nullable
    private JsExpression translateInt() {
        if (right instanceof KtBinaryExpression) {
            KtBinaryExpression binary = (KtBinaryExpression) right;
            if (binary.getOperationToken() != KtTokens.RANGE) {
                return null;
            }
            KtExpression lower = binary.getLeft();
            KtExpression upper = binary.getRight();
            assert lower != null && upper != null : "Seems that frontend has finished with error";
            return translateInt(lower, upper);
        } else if (right instanceof KtQualifiedExpression) {
            KtQualifiedExpression qualified = (KtQualifiedExpression) right;
            KtExpression lower = qualified.getReceiverExpression();
            KtExpression possibleCall = qualified.getSelectorExpression();
            if (possibleCall instanceof KtCallExpression) {
                if (!INT_RANGE_TEST.apply(getResolvedFunctionCall(possibleCall).getResultingDescriptor())) {
                    return null;
                }
                KtCallExpression call = (KtCallExpression) possibleCall;
                KtExpression upper = call.getValueArguments().get(0).getArgumentExpression();
                assert upper != null : "Seems that frontend has finished with error";
                return translateInt(lower, upper);
            }
        }
        return null;
    }

    @NotNull
    private JsExpression translateInt(@NotNull KtExpression lowerExpression, @NotNull KtExpression upperExpression) {
        JsExpression lower = Translation.translateAsExpression(lowerExpression, context());
        JsExpression upper = Translation.translateAsExpression(upperExpression, context());
        JsExpression first = JsAstUtils.greaterThanEq(left, lower);
        JsExpression second = JsAstUtils.lessThanEq(left, upper);
        return JsAstUtils.and(first, second);
    }
}
