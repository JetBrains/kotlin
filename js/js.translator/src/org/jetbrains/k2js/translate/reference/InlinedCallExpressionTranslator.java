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

package org.jetbrains.k2js.translate.reference;

import com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.ResolvedValueArgument;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCallForCallExpression;

/**
 * @author Pavel Talanov
 */
public final class InlinedCallExpressionTranslator extends AbstractCallExpressionTranslator {

    public static boolean shouldBeInlined(@NotNull JetCallExpression expression, @NotNull TranslationContext context) {
        ResolvedCall<?> resolvedCall = getResolvedCallForCallExpression(context.bindingContext(), expression);
        CallableDescriptor descriptor = resolvedCall.getCandidateDescriptor();
        if (descriptor instanceof SimpleFunctionDescriptor) {
            return ((SimpleFunctionDescriptor)descriptor).isInline();
        }
        return false;
    }

    @NotNull
    public static JsExpression translate(@NotNull JetCallExpression expression,
                                         @Nullable JsExpression receiver,
                                         @NotNull CallType callType,
                                         @NotNull TranslationContext context) {
        return (new InlinedCallExpressionTranslator(expression, receiver, callType, context)).translate();
    }

    private InlinedCallExpressionTranslator(@NotNull JetCallExpression expression, @Nullable JsExpression receiver,
                                            @NotNull CallType callType, @NotNull TranslationContext context) {
        super(expression, receiver, callType, context);
    }

    @NotNull
    private JsExpression translate() {
        TranslationContext contextWithAllParametersAliased = createContextWithAllParametersAliased();
        JetFunction function = BindingUtils.getFunctionForDescriptor(bindingContext(), getFunctionDescriptor());
        return Translation.translateAsExpression(function.getBodyExpression(), contextWithAllParametersAliased);
    }

    @NotNull
    private SimpleFunctionDescriptor getFunctionDescriptor() {
        CallableDescriptor descriptor = resolvedCall.getCandidateDescriptor().getOriginal();
        assert descriptor instanceof SimpleFunctionDescriptor : "Inlined functions should have descriptor of type SimpleFunctionDescriptor";
        return (SimpleFunctionDescriptor)descriptor;
    }

    private TranslationContext createContextWithAllParametersAliased() {
        Map<DeclarationDescriptor, JsName> aliases = Maps.newHashMap();
        for (ValueParameterDescriptor parameterDescriptor : resolvedCall.getResultingDescriptor().getValueParameters()) {
            ResolvedValueArgument actualArgument = resolvedCall.getValueArgumentsByIndex().get(parameterDescriptor.getIndex());
            JsExpression translatedArgument = translateArgument(parameterDescriptor, actualArgument);
            TemporaryVariable aliasForArgument = context().declareTemporary(translatedArgument);
            aliases.put(parameterDescriptor, aliasForArgument.name());
            context().addStatementToCurrentBlock(aliasForArgument.assignmentExpression().makeStmt());
        }
        return context().innerContextWithDescriptorsAliased(aliases);
    }

    @NotNull
    private JsExpression translateArgument(@NotNull ValueParameterDescriptor parameterDescriptor,
                                           @NotNull ResolvedValueArgument actualArgument) {
        List<JsExpression> translatedSingleArgument = translateSingleArgument(actualArgument, parameterDescriptor);
        assert translatedSingleArgument.size() == 1 : "We always wrap varargs in kotlin calls.";
        return translatedSingleArgument.get(0);
    }

    @Override
    public boolean shouldWrapVarargInArray() {
        return true;
    }
}
