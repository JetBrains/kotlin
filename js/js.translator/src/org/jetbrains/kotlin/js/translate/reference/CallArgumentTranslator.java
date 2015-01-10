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

package org.jetbrains.kotlin.js.translate.reference;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.context.TemporaryConstVariable;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;

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

        public ArgumentsInfo(List<JsExpression> arguments, boolean operator, TemporaryConstVariable receiver) {
            translateArguments = arguments;
            hasSpreadOperator = operator;
            cachedReceiver = receiver;
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
    }

    private static enum ArgumentsKind { HAS_EMPTY_EXPRESSION_ARGUMENT, HAS_NOT_EMPTY_EXPRESSION_ARGUMENT }

    @NotNull
    private static ArgumentsKind translateSingleArgument(
            @NotNull ResolvedValueArgument actualArgument,
            @NotNull List<JsExpression> result,
            @NotNull TranslationContext context
    ) {
        List<ValueArgument> valueArguments = actualArgument.getArguments();

        if (actualArgument instanceof DefaultValueArgument) {
            result.add(context.namer().getUndefinedExpression());
            return ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT;
        }

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

    @NotNull
    private static ArgumentsKind translateVarargArgument(
            @NotNull List<ValueArgument> arguments,
            @NotNull List<JsExpression> result,
            @NotNull TranslationContext context,
            boolean shouldWrapVarargInArray
    ) {
        if (arguments.isEmpty()) {
            if (shouldWrapVarargInArray) {
                result.add(new JsArrayLiteral(Collections.<JsExpression>emptyList()));
            }
            return ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT;
        }

        List<JsExpression> list;
        if (shouldWrapVarargInArray) {
            list = arguments.size() == 1 ? new SmartList<JsExpression>() : new ArrayList<JsExpression>(arguments.size());
        }
        else {
            list = result;
        }

        ArgumentsKind resultKind = translateValueArguments(arguments, list, context);

        if (shouldWrapVarargInArray) {
            List<JsExpression> concatArguments = prepareConcatArguments(arguments, list);
            JsExpression concatExpression = concatArgumentsIfNeeded(concatArguments);
            result.add(concatExpression);
        }

        return resultKind;
    }

    private static ArgumentsKind translateValueArguments(
            @NotNull List<ValueArgument> arguments,
            @NotNull List<JsExpression> list,
            @NotNull TranslationContext context
    ) {
        ArgumentsKind resultKind = ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT;
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
            extractArguments(list, argContexts, context, resultKind == ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT);
        }
        return resultKind;
    }

    @NotNull
    private static JsExpression concatArgumentsIfNeeded(@NotNull List<JsExpression> concatArguments) {
        assert concatArguments.size() > 0 : "concatArguments.size should not be 0";

        if (concatArguments.size() > 1) {
            return new JsInvocation(new JsNameRef("concat", concatArguments.get(0)), concatArguments.subList(1, concatArguments.size()));

        }
        else {
            return concatArguments.get(0);
        }
    }

    @NotNull
    private static List<JsExpression> prepareConcatArguments(@NotNull List<ValueArgument> arguments, @NotNull List<JsExpression> list) {
        assert arguments.size() != 0 : "arguments.size should not be 0";
        assert arguments.size() == list.size() : "arguments.size: " + arguments.size() + " != list.size: " + list.size();

        List<JsExpression> concatArguments = new SmartList<JsExpression>();
        List<JsExpression> lastArrayContent = new SmartList<JsExpression>();

        int size = arguments.size();
        for(int index = 0; index < size; index++) {
            ValueArgument valueArgument = arguments.get(index);
            JsExpression expressionArgument = list.get(index);

            if (valueArgument.getSpreadElement() != null) {
                if (lastArrayContent.size() > 0) {
                    concatArguments.add(new JsArrayLiteral(lastArrayContent));
                    concatArguments.add(expressionArgument);
                    lastArrayContent = new SmartList<JsExpression>();
                }
                else {
                    concatArguments.add(expressionArgument);
                }
            }
            else {
                lastArrayContent.add(expressionArgument);
            }
        }
        if (lastArrayContent.size() > 0) {
            concatArguments.add(new JsArrayLiteral(lastArrayContent));
        }

        return concatArguments;
    }

    private static void extractArguments(
            @NotNull List<JsExpression> argExpressions,
            @NotNull List<TranslationContext> argContexts,
            @NotNull TranslationContext context,
            boolean toTmpVars
    ) {
        for(int i=0; i<argExpressions.size(); i++) {
            TranslationContext argContext = argContexts.get(i);
            JsExpression jsArgExpression = argExpressions.get(i);
            if (argContext.currentBlockIsEmpty() && TranslationUtils.isCacheNeeded(jsArgExpression)) {
                if (toTmpVars) {
                    TemporaryVariable temporaryVariable = context.declareTemporary(jsArgExpression);
                    context.addStatementToCurrentBlock(temporaryVariable.assignmentExpression().makeStmt());
                    argExpressions.set(i, temporaryVariable.reference());
                }
                else {
                    context.addStatementToCurrentBlock(jsArgExpression.makeStmt());
                }
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
        ArgumentsKind kind = ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT;
        List<JsExpression> concatArguments = null;

        for (ValueParameterDescriptor parameterDescriptor : valueParameters) {
            ResolvedValueArgument actualArgument = valueArgumentsByIndex.get(parameterDescriptor.getIndex());

            TranslationContext argContext = context().innerBlock();

            if (actualArgument instanceof VarargValueArgument) {

                List<ValueArgument> arguments = actualArgument.getArguments();

                int size = arguments.size();
                for (int i = 0; i != size; ++i) {
                    if (arguments.get(i).getSpreadElement() != null) {
                        hasSpreadOperator = true;
                        break;
                    }
                }

                if (hasSpreadOperator) {
                    if (isNativeFunctionCall) {
                        argsBeforeVararg = result;
                        result = new SmartList<JsExpression>();
                        List<JsExpression> list = new SmartList<JsExpression>();
                        kind = translateValueArguments(arguments, list, argContext);
                        concatArguments = prepareConcatArguments(arguments, list);
                    }
                    else {
                        kind = translateVarargArgument(arguments, result, argContext, size > 1);
                    }
                }
                else {
                    kind = translateVarargArgument(arguments, result, argContext, !isNativeFunctionCall);
                }
            }
            else {
                kind = translateSingleArgument(actualArgument, result, argContext);
            }

            context().moveVarsFrom(argContext);
            argContexts.add(argContext);
            argumentsShouldBeExtractedToTmpVars = argumentsShouldBeExtractedToTmpVars || !argContext.currentBlockIsEmpty();

            if (kind == ArgumentsKind.HAS_EMPTY_EXPRESSION_ARGUMENT) break;
        }

        if (argumentsShouldBeExtractedToTmpVars) {
            extractArguments(result, argContexts, context(), kind == ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT);
        }

        if (isNativeFunctionCall && hasSpreadOperator) {
            assert argsBeforeVararg != null : "argsBeforeVararg should not be null";
            assert concatArguments != null : "concatArguments should not be null";

            concatArguments.addAll(result);

            if (!argsBeforeVararg.isEmpty()) {
                concatArguments.add(0, new JsArrayLiteral(argsBeforeVararg));
            }

            result = new SmartList<JsExpression>(concatArgumentsIfNeeded(concatArguments));

            if (receiver != null) {
                cachedReceiver = context().getOrDeclareTemporaryConstVariable(receiver);
                result.add(0, cachedReceiver.reference());
            }
            else {
                result.add(0, JsLiteral.NULL);
            }
        }

        removeLastUndefinedArguments(result);
        return new ArgumentsInfo(result, hasSpreadOperator, cachedReceiver);
    }

}
