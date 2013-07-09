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

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsArrayLiteral;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDefaultArgument;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCallForCallExpression;

public abstract class AbstractCallExpressionTranslator extends AbstractTranslator {

    @NotNull
    protected final JetCallExpression expression;
    @NotNull
    protected final ResolvedCall<?> resolvedCall;
    @Nullable
    protected final JsExpression receiver;
    @NotNull
    protected final CallType callType;

    protected AbstractCallExpressionTranslator(@NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull CallType type, @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.resolvedCall = getResolvedCallForCallExpression(bindingContext(), expression);
        this.receiver = receiver;
        this.callType = type;
    }

    protected abstract boolean shouldWrapVarargInArray();

    @NotNull
    protected List<JsExpression> translateSingleArgument(@NotNull ResolvedValueArgument actualArgument,
            @NotNull ValueParameterDescriptor parameterDescriptor) {
        List<ValueArgument> valueArguments = actualArgument.getArguments();
        if (actualArgument instanceof VarargValueArgument) {
            return translateVarargArgument(valueArguments);
        }
        if (actualArgument instanceof DefaultValueArgument) {
            JetExpression defaultArgument = getDefaultArgument(bindingContext(), parameterDescriptor);
            return Collections.singletonList(Translation.translateAsExpression(defaultArgument, context()));
        }
        assert actualArgument instanceof ExpressionValueArgument;
        assert valueArguments.size() == 1;
        JetExpression argumentExpression = valueArguments.get(0).getArgumentExpression();
        assert argumentExpression != null;
        return Collections.singletonList(Translation.translateAsExpression(argumentExpression, context()));
    }

    @NotNull
    private List<JsExpression> translateVarargArgument(@NotNull List<ValueArgument> arguments) {
        List<JsExpression> translatedArgs = Lists.newArrayList();
        for (ValueArgument argument : arguments) {
            JetExpression argumentExpression = argument.getArgumentExpression();
            assert argumentExpression != null;
            translatedArgs.add(Translation.translateAsExpression(argumentExpression, context()));
        }
        if (shouldWrapVarargInArray()) {
            return wrapInArrayLiteral(translatedArgs);
        }
        return translatedArgs;
    }

    @NotNull
    private static List<JsExpression> wrapInArrayLiteral(@NotNull List<JsExpression> translatedArgs) {
        return Collections.<JsExpression>singletonList(new JsArrayLiteral(translatedArgs));
    }
}
