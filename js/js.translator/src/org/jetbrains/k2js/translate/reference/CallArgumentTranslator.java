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

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.k2js.translate.context.TemporaryConstVariable;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CallArgumentTranslator extends AbstractTranslator {

    @NotNull
    public static ArgumentsInfo translate(
            @NotNull ResolvedCall<?> resolvedCall,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        return translate(resolvedCall, receiver, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static ArgumentsInfo translate(
            @NotNull ResolvedCall<?> resolvedCall,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context,
            @NotNull JsBlock block
    ) {
        TranslationContext innerContext = context.innerBlock(block);
        CallArgumentTranslator argumentTranslator = new CallArgumentTranslator(resolvedCall, receiver, innerContext);
        ArgumentsInfo result = argumentTranslator.translate();
        context.moveVarsFrom(innerContext);
        return result;
    }

    public static class ArgumentsInfo {
        private final List<JsExpression> translateArguments;
        private final boolean hasSpreadOperator;
        private final TemporaryConstVariable cachedReceiver;
        private final boolean hasEmptyExpressionArgument;

        public ArgumentsInfo(List<JsExpression> arguments, boolean operator, TemporaryConstVariable receiver) {
            this(arguments, operator, receiver, false);
        }

        public ArgumentsInfo(List<JsExpression> arguments, boolean operator, TemporaryConstVariable receiver, boolean hasEmptyExpressionArgument) {
            translateArguments = arguments;
            hasSpreadOperator = operator;
            cachedReceiver = receiver;
            this.hasEmptyExpressionArgument = hasEmptyExpressionArgument;
        }

        @NotNull
        public List<JsExpression> getTranslateArguments() {
            return translateArguments;
        }

        public boolean isHasSpreadOperator() {
            return hasSpreadOperator;
        }

        @Nullable
        public TemporaryConstVariable getCachedReceiver() {
            return cachedReceiver;
        }

        public boolean hasEmptyExpressionArgument() {
            return hasEmptyExpressionArgument;
        }
    }

    public static enum ArgumentsKind { HAS_EMPTY_EXPRESSION_ARGUMENT, HAS_NOT_EMPTY_EXPRESSION_ARGUMENT }

    @NotNull
    public static ArgumentsKind translateSingleArgument(
            @NotNull ResolvedValueArgument actualArgument,
            @NotNull List<JsExpression> result,
            @NotNull TranslationContext context,
            boolean shouldWrapVarargInArray
    ) {
        List<ValueArgument> valueArguments = actualArgument.getArguments();
        if (actualArgument instanceof VarargValueArgument) {
            return translateVarargArgument(valueArguments, result, context, shouldWrapVarargInArray);
        }
        else if (actualArgument instanceof DefaultValueArgument) {
            result.add(context.namer().getUndefinedExpression());
            return ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT;
        }
        else {
            assert actualArgument instanceof ExpressionValueArgument;
            assert valueArguments.size() == 1;
            JetExpression argumentExpression = valueArguments.get(0).getArgumentExpression();
            assert argumentExpression != null;
            JsExpression jsExpression = Translation.translateAsExpression(argumentExpression, context);
            result.add(jsExpression);
            if (JsAstUtils.isEmptyExpression(jsExpression)) {
                return ArgumentsKind.HAS_EMPTY_EXPRESSION_ARGUMENT;
            }
            else {
                return ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT;
            }
        }
    }

    @NotNull
    private static ArgumentsKind translateVarargArgument(
            @NotNull List<ValueArgument> arguments,
            @NotNull List<JsExpression> result,
            @NotNull TranslationContext context,
            boolean shouldWrapVarargInArray
    ) {
        ArgumentsKind resultKind = ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT;
        if (arguments.isEmpty()) {
            if (shouldWrapVarargInArray) {
                result.add(new JsArrayLiteral(Collections.<JsExpression>emptyList()));
            }
            return ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT;
        }

        List<JsExpression> list;
        if (shouldWrapVarargInArray) {
            list = arguments.size() == 1 ? new SmartList<JsExpression>() : new ArrayList<JsExpression>(arguments.size());
            result.add(new JsArrayLiteral(list));
        }
        else {
            list = result;
        }
        List<TranslationContext> argContexts = new SmartList<TranslationContext>();
        boolean argumentsShouldBeExtractedToTmpVars = false;
        for (ValueArgument argument : arguments) {
            JetExpression argumentExpression = argument.getArgumentExpression();
            assert argumentExpression != null;
            TranslationContext argContext = context.innerBlock();
            JsExpression argExpression = Translation.translateAsExpression(argumentExpression, argContext);
            list.add(argExpression);
            context.moveVarsFrom(argContext);
            argContexts.add(argContext);
            argumentsShouldBeExtractedToTmpVars = argumentsShouldBeExtractedToTmpVars || !argContext.currentBlockIsEmpty();
            if (JsAstUtils.isEmptyExpression(argExpression)) {
                resultKind = ArgumentsKind.HAS_EMPTY_EXPRESSION_ARGUMENT;
                break;
            }
        }
        if (argumentsShouldBeExtractedToTmpVars) {
            extractArgumentsToTmpVars(list, argContexts, context);
        }
        return resultKind;
    }

    private static void extractArgumentsToTmpVars(
            @NotNull List<JsExpression> argExpressions,
            @NotNull List<TranslationContext> argContexts,
            @NotNull TranslationContext context
    ) {
        for(int i=0; i<argExpressions.size(); i++) {
            TranslationContext argContext = argContexts.get(i);
            JsExpression jsArgExpression = argExpressions.get(i);
            if (argContext.currentBlockIsEmpty() && TranslationUtils.isCacheNeeded(jsArgExpression)) {
                TemporaryVariable temporaryVariable = context.declareTemporary(jsArgExpression);
                context.addStatementToCurrentBlock(temporaryVariable.assignmentExpression().makeStmt());
                argExpressions.set(i, temporaryVariable.reference());
            } else {
                context.addStatementsToCurrentBlockFrom(argContext);
            }
        }
    }

    @NotNull
    private final ResolvedCall<?> resolvedCall;
    @Nullable
    private final JsExpression receiver;
    private final boolean isNativeFunctionCall;

    private CallArgumentTranslator(
            @NotNull ResolvedCall<?> resolvedCall,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        super(context);
        this.resolvedCall = resolvedCall;
        this.receiver = receiver;
        this.isNativeFunctionCall = AnnotationsUtils.isNativeObject(resolvedCall.getCandidateDescriptor());
    }

    private void removeLastUndefinedArguments(@NotNull List<JsExpression> result) {
        int i;
        for (i = result.size() - 1; i >= 0; i--) {
            if (result.get(i) != context().namer().getUndefinedExpression()) {
                break;
            }
        }
        result.subList(i + 1, result.size()).clear();
    }

    private ArgumentsInfo translate() {
        boolean hasEmptyExpressionArgument = false;
        List<ValueParameterDescriptor> valueParameters = resolvedCall.getResultingDescriptor().getValueParameters();
        if (valueParameters.isEmpty()) {
            return new ArgumentsInfo(Collections.<JsExpression>emptyList(), false, null);
        }
        boolean hasSpreadOperator = false;
        TemporaryConstVariable cachedReceiver = null;

        List<JsExpression> result = new ArrayList<JsExpression>(valueParameters.size());
        List<ResolvedValueArgument> valueArgumentsByIndex = resolvedCall.getValueArgumentsByIndex();
        if (valueArgumentsByIndex == null) {
            throw new IllegalStateException("Failed to arrange value arguments by index: " + resolvedCall.getResultingDescriptor());
        }
        List<JsExpression> argsBeforeVararg = null;
        boolean argumentsShouldBeExtractedToTmpVars = false;
        List<TranslationContext> argContexts = new SmartList<TranslationContext>();

        for (ValueParameterDescriptor parameterDescriptor : valueParameters) {
            ResolvedValueArgument actualArgument = valueArgumentsByIndex.get(parameterDescriptor.getIndex());

            if (actualArgument instanceof VarargValueArgument) {
                assert !hasSpreadOperator;

                List<ValueArgument> arguments = actualArgument.getArguments();
                hasSpreadOperator = arguments.size() == 1 && arguments.get(0).getSpreadElement() != null;

                if (isNativeFunctionCall && hasSpreadOperator) {
                    argsBeforeVararg = result;
                    result = new SmartList<JsExpression>();
                }
            }
            TranslationContext argContext = context().innerBlock();
            ArgumentsKind kind = translateSingleArgument(actualArgument, result, argContext, !isNativeFunctionCall && !hasSpreadOperator);
            context().moveVarsFrom(argContext);
            argContexts.add(argContext);
            argumentsShouldBeExtractedToTmpVars = argumentsShouldBeExtractedToTmpVars || !argContext.currentBlockIsEmpty();

            if (kind == ArgumentsKind.HAS_EMPTY_EXPRESSION_ARGUMENT) {
                hasEmptyExpressionArgument = true;
                break;
            }
        }

        if (argumentsShouldBeExtractedToTmpVars) {
            extractArgumentsToTmpVars(result, argContexts, context());
        }

        if (isNativeFunctionCall && hasSpreadOperator) {
            if (!argsBeforeVararg.isEmpty()) {
                JsInvocation concatArguments = new JsInvocation(new JsNameRef("concat", new JsArrayLiteral(argsBeforeVararg)), result);
                result = new SmartList<JsExpression>(concatArguments);
            }

            if (receiver != null) {
                cachedReceiver = context().getOrDeclareTemporaryConstVariable(receiver);
                result.add(0, cachedReceiver.reference());
            }
            else {
                result.add(0, JsLiteral.NULL);
            }
        }

        removeLastUndefinedArguments(result);
        return new ArgumentsInfo(result, hasSpreadOperator, cachedReceiver, hasEmptyExpressionArgument);
    }

}
