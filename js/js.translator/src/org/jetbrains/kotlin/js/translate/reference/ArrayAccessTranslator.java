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

import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.psi.KtArrayAccessExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayAccessTranslator extends AbstractTranslator implements AccessTranslator {

    /*package*/
    static ArrayAccessTranslator newInstance(@NotNull KtArrayAccessExpression expression,
                                             @NotNull TranslationContext context) {
        return new ArrayAccessTranslator(expression, context);
    }

    @NotNull
    private final KtArrayAccessExpression expression;

    private ArrayAccessTranslator(@NotNull KtArrayAccessExpression expression, @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
    }

    @NotNull
    @Override
    public JsExpression translateAsGet() {
        return translateAsGet(getArrayExpression());
    }

    @NotNull
    protected JsExpression translateAsGet(@NotNull JsExpression arrayExpression) {
        return translateAsMethodCall(arrayExpression,  null);
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        return translateAsMethodCall(getArrayExpression(), setTo);
    }

    @NotNull
    private JsExpression translateAsMethodCall(@NotNull JsExpression arrayExpression, @Nullable JsExpression toSetTo) {
        boolean isGetter = toSetTo == null;
        TranslationContext context = context();
        ResolvedCall<FunctionDescriptor> resolvedCall = BindingUtils.getResolvedCallForArrayAccess(bindingContext(), expression, isGetter);
        if (!isGetter) {
            context = contextWithValueParameterAliasInArrayGetAccess(toSetTo);
        }
        return CallTranslator.translate(context, resolvedCall, arrayExpression);
    }

    @NotNull
    protected JsExpression getArrayExpression() {
        KtExpression arrayExpression = expression.getArrayExpression();
        assert arrayExpression != null : "Code with parsing errors shouldn't be translated";
        return Translation.translateAsExpression(arrayExpression, context());
    }

    // this is hack for a[b]++ -> a.set(b, a.get(b) + 1). Frontend generate fake expression for a.get(b) + 1.
    @NotNull
    private TranslationContext contextWithValueParameterAliasInArrayGetAccess(@NotNull JsExpression toSetTo) {
        ResolvedCall<FunctionDescriptor> resolvedCall =
                BindingUtils.getResolvedCallForArrayAccess(bindingContext(), expression,  /*isGetter = */ false);

        List<ResolvedValueArgument> arguments = resolvedCall.getValueArgumentsByIndex();
        if (arguments == null) {
            throw new IllegalStateException("Failed to arrange value arguments by index: " + resolvedCall.getResultingDescriptor());
        }
        ResolvedValueArgument lastArgument = arguments.get(arguments.size() - 1);
        assert lastArgument instanceof ExpressionValueArgument:
                "Last argument of array-like setter must be ExpressionValueArgument: " + lastArgument;

        ValueArgument valueArgument = ((ExpressionValueArgument) lastArgument).getValueArgument();
        assert valueArgument != null;

        KtExpression element = valueArgument.getArgumentExpression();
        return context().innerContextWithAliasesForExpressions(Collections.singletonMap(element, toSetTo));
    }

    @NotNull
    @Override
    public AccessTranslator getCached() {
        Map<KtExpression, JsExpression> aliases = new HashMap<>();

        JsExpression arrayExpression = context().cacheExpressionIfNeeded(getArrayExpression());
        aliases.put(expression.getArrayExpression(), arrayExpression);

        for (KtExpression ktExpression : expression.getIndexExpressions()) {
            JsExpression jsExpression = context().cacheExpressionIfNeeded(Translation.translateAsExpression(ktExpression, context()));
            aliases.put(ktExpression, jsExpression);
        }

        return new CachedArrayAccessTranslator(expression, context().innerContextWithAliasesForExpressions(aliases), arrayExpression);
    }

    private static class CachedArrayAccessTranslator extends ArrayAccessTranslator  {
        @NotNull
        private final JsExpression arrayExpression;

        protected CachedArrayAccessTranslator(
                @NotNull KtArrayAccessExpression expression,
                @NotNull TranslationContext context,
                @NotNull JsExpression arrayExpression
        ) {
            super(expression, context);
            this.arrayExpression = arrayExpression;
        }

        @NotNull
        @Override
        protected JsExpression getArrayExpression() {
            return arrayExpression;
        }

        @NotNull
        @Override
        public AccessTranslator getCached() {
            return this;
        }
    }
}
