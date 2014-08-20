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

import com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsLiteral;
import com.google.dart.compiler.backend.js.ast.JsNode;
import com.google.dart.compiler.backend.js.ast.JsReturn;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.k2js.translate.callTranslator.CallInfo;
import org.jetbrains.k2js.translate.callTranslator.CallTranslatorPackage;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.mutator.LastExpressionMutator;
import org.jetbrains.k2js.translate.utils.mutator.Mutator;

import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.translate.reference.CallArgumentTranslator.translateSingleArgument;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionForDescriptor;
import static org.jetbrains.k2js.translate.utils.FunctionBodyTranslator.translateFunctionBody;

public final class InlinedCallExpressionTranslator extends AbstractCallExpressionTranslator {

    @SuppressWarnings("UnusedParameters")
    public static boolean shouldBeInlined(@NotNull JetCallExpression expression, @NotNull TranslationContext context) {
        //TODO: inlining turned off
        //ResolvedCall<?> resolvedCall = CallUtilPackage.getFunctionResolvedCall(expression, bindingContext());
        //CallableDescriptor descriptor = resolvedCall.getCandidateDescriptor();
        //if (descriptor instanceof SimpleFunctionDescriptor) {
        //    return ((SimpleFunctionDescriptor)descriptor).isInline();
        //}
        return false;
    }

    @NotNull
    public static JsExpression translate(
            @NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        return (new InlinedCallExpressionTranslator(expression, receiver, context)).translate();
    }

    private InlinedCallExpressionTranslator(
            @NotNull JetCallExpression expression, @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        super(expression, receiver, context);
    }

    @NotNull
    private JsExpression translate() {
        TranslationContext contextWithAllParametersAliased = createContextForInlining();
        JsNode translatedBody = translateFunctionBody(getFunctionDescriptor(), getFunctionBody(), contextWithAllParametersAliased);
        //TODO: declare uninitialized temporary
        TemporaryVariable temporaryVariable = contextWithAllParametersAliased.declareTemporary(JsLiteral.NULL);
        JsNode mutatedBody = LastExpressionMutator.mutateLastExpression(translatedBody, new InlineFunctionMutator(temporaryVariable));
        context().addStatementToCurrentBlock(JsAstUtils.convertToBlock(mutatedBody));
        return temporaryVariable.reference();
    }

    @NotNull
    private JetFunction getFunctionBody() {
        return getFunctionForDescriptor(getFunctionDescriptor());
    }

    @NotNull
    private SimpleFunctionDescriptor getFunctionDescriptor() {
        CallableDescriptor descriptor = resolvedCall.getCandidateDescriptor().getOriginal();
        assert descriptor instanceof SimpleFunctionDescriptor : "Inlined functions should have descriptor of type SimpleFunctionDescriptor";
        return (SimpleFunctionDescriptor)descriptor;
    }

    @NotNull
    private TranslationContext createContextForInlining() {
        TranslationContext contextForInlining = context();
        contextForInlining = createContextWithAliasForThisExpression(contextForInlining);
        return createContextWithAliasesForParameters(contextForInlining);
    }

    @NotNull
    private TranslationContext createContextWithAliasesForParameters(@NotNull TranslationContext contextForInlining) {
        Map<DeclarationDescriptor, JsExpression> aliases = Maps.newHashMap();
        for (ValueParameterDescriptor parameterDescriptor : resolvedCall.getResultingDescriptor().getValueParameters()) {
            TemporaryVariable aliasForArgument = createAliasForArgument(parameterDescriptor);
            aliases.put(parameterDescriptor, aliasForArgument.name().makeRef());
        }
        return contextForInlining.innerContextWithDescriptorsAliased(aliases);
    }

    @NotNull
    private TranslationContext createContextWithAliasForThisExpression(@NotNull TranslationContext contextForInlining) {
        TranslationContext contextWithAliasForThisExpression = contextForInlining;
        SimpleFunctionDescriptor functionDescriptor = getFunctionDescriptor();
        CallInfo callInfo = CallTranslatorPackage.getCallInfo(contextForInlining, resolvedCall, receiver);
        JsExpression receiver = callInfo.getReceiverObject();
        if (receiver != null) {
            contextWithAliasForThisExpression =
                contextWithAlias(contextWithAliasForThisExpression, receiver, functionDescriptor.getReceiverParameter());
        }
        JsExpression thisObject = callInfo.getThisObject();
        if (thisObject != null) {
            contextWithAliasForThisExpression =
                contextWithAlias(contextWithAliasForThisExpression, thisObject, functionDescriptor.getExpectedThisObject());
        }
        return contextWithAliasForThisExpression;
    }

    @NotNull
    private TranslationContext contextWithAlias(@NotNull TranslationContext contextWithAliasForThisExpression,
                                                @NotNull JsExpression aliasExpression, @Nullable DeclarationDescriptor descriptorToAlias) {
        TemporaryVariable aliasForReceiver = context().declareTemporary(aliasExpression);
        assert descriptorToAlias != null;
        TranslationContext newContext =
                contextWithAliasForThisExpression.innerContextWithAliased(descriptorToAlias, aliasForReceiver.reference());
        newContext.addStatementToCurrentBlock(aliasForReceiver.assignmentExpression().makeStmt());
        return newContext;
    }

    @NotNull
    private TemporaryVariable createAliasForArgument(@NotNull ValueParameterDescriptor parameterDescriptor) {
        List<ResolvedValueArgument> actualArguments = resolvedCall.getValueArgumentsByIndex();
        if (actualArguments == null) {
            throw new IllegalStateException("Failed to arrange value arguments by index: " + resolvedCall.getResultingDescriptor());
        }
        ResolvedValueArgument actualArgument = actualArguments.get(parameterDescriptor.getIndex());
        JsExpression translatedArgument = translateArgument(actualArgument);
        TemporaryVariable aliasForArgument = context().declareTemporary(translatedArgument);
        context().addStatementToCurrentBlock(aliasForArgument.assignmentExpression().makeStmt());
        return aliasForArgument;
    }

    @NotNull
    private JsExpression translateArgument(@NotNull ResolvedValueArgument actualArgument) {
        List<JsExpression> result = new SmartList<JsExpression>();
        translateSingleArgument(actualArgument, result, context(), true);
        assert result.size() == 1 : "We always wrap varargs in kotlin calls.";
        return result.get(0);
    }

    private static final class InlineFunctionMutator implements Mutator {

        @NotNull
        private final TemporaryVariable toAssignTo;

        private InlineFunctionMutator(@NotNull TemporaryVariable to) {
            toAssignTo = to;
        }

        @NotNull
        @Override
        public JsNode mutate(@NotNull JsNode node) {
            if (node instanceof JsReturn) {
                JsExpression returnedExpression = ((JsReturn)node).getExpression();
                return JsAstUtils.assignment(toAssignTo.name().makeRef(), returnedExpression);
            }
            return node;
        }
    }
}
