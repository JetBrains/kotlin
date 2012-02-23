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

package org.jetbrains.k2js.translate.reference;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsArrayLiteral;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDefaultArgument;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCallForCallExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getCallee;

/**
 * @author Pavel Talanov
 */
public final class CallExpressionTranslator extends AbstractTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetCallExpression expression,
                                         @Nullable JsExpression receiver,
                                         @NotNull CallType callType,
                                         @NotNull TranslationContext context) {
        return (new CallExpressionTranslator(expression, context)).translate(receiver, callType);
    }

    @NotNull
    private final JetCallExpression expression;
    @NotNull
    private final ResolvedCall<?> resolvedCall;
    private final boolean isNativeFunctionCall;

    private CallExpressionTranslator(@NotNull JetCallExpression expression,
                                     @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.resolvedCall = getResolvedCallForCallExpression(bindingContext(), expression);
        this.isNativeFunctionCall = AnnotationsUtils.isNativeObject(resolvedCall.getCandidateDescriptor());
    }

    @NotNull
    private JsExpression translate(@Nullable JsExpression receiver,
                                   @NotNull CallType callType) {
        return CallBuilder.build(context())
                .receiver(receiver)
                .callee(getCalleeExpression())
                .args(translateArguments())
                .resolvedCall(resolvedCall)
                .type(callType)
                .translate();
    }

    @Nullable
    private JsExpression getCalleeExpression() {
        if (resolvedCall.getCandidateDescriptor() instanceof ExpressionAsFunctionDescriptor) {
            return Translation.translateAsExpression(getCallee(expression), context());
        }
        return null;
    }

    @NotNull
    private List<JsExpression> translateArguments() {
        List<JsExpression> result = new ArrayList<JsExpression>();
        ResolvedCall<?> resolvedCall = getResolvedCallForCallExpression(bindingContext(), expression);
        for (ValueParameterDescriptor parameterDescriptor : resolvedCall.getResultingDescriptor().getValueParameters()) {
            ResolvedValueArgument actualArgument = resolvedCall.getValueArgumentsByIndex().get(parameterDescriptor.getIndex());
            result.addAll(translateSingleArgument(actualArgument, parameterDescriptor));
        }
        return result;
    }

    @NotNull
    private List<JsExpression> translateSingleArgument(@NotNull ResolvedValueArgument actualArgument,
                                                       @NotNull ValueParameterDescriptor parameterDescriptor) {
        List<JetExpression> argumentExpressions = actualArgument.getArgumentExpressions();
        if (actualArgument instanceof VarargValueArgument) {
            return translateVarargArgument(argumentExpressions);
        }
        if (actualArgument instanceof DefaultValueArgument) {
            JetExpression defaultArgument = getDefaultArgument(bindingContext(), parameterDescriptor);
            return Arrays.asList(Translation.translateAsExpression(defaultArgument, context()));
        }
        assert actualArgument instanceof ExpressionValueArgument;
        assert argumentExpressions.size() == 1;
        return Arrays.asList(Translation.translateAsExpression(argumentExpressions.get(0), context()));
    }

    @NotNull
    private List<JsExpression> translateVarargArgument(@NotNull List<JetExpression> arguments) {
        List<JsExpression> translatedArgs = Lists.newArrayList();
        for (JetExpression argument : arguments) {
            translatedArgs.add(Translation.translateAsExpression(argument, context()));
        }
        if (isNativeFunctionCall) {
            return translatedArgs;
        }
        return wrapInArrayLiteral(translatedArgs);
    }

    @NotNull
    private List<JsExpression> wrapInArrayLiteral(@NotNull List<JsExpression> translatedArgs) {
        JsArrayLiteral argsWrappedInArray = new JsArrayLiteral();
        argsWrappedInArray.getExpressions().addAll(translatedArgs);
        return Arrays.<JsExpression>asList(argsWrappedInArray);
    }
}
