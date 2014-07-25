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
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.k2js.translate.callTranslator.CallTranslator;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.*;

public class ArrayAccessTranslator extends AbstractTranslator implements AccessTranslator {

    /*package*/
    static ArrayAccessTranslator newInstance(@NotNull JetArrayAccessExpression expression,
                                             @NotNull TranslationContext context) {
        return new ArrayAccessTranslator(expression, context);
    }

    @NotNull
    private final JetArrayAccessExpression expression;

    protected ArrayAccessTranslator(@NotNull JetArrayAccessExpression expression,
                                    @NotNull TranslationContext context) {
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
        return translateAsSet(getArrayExpression(), setTo);
    }

    @NotNull
    protected JsExpression translateAsSet(@NotNull JsExpression arrayExpression, @NotNull JsExpression toSetTo) {
        return translateAsMethodCall(arrayExpression,  toSetTo);
    }

    @NotNull
    private JsExpression translateAsMethodCall(@NotNull JsExpression arrayExpression, @Nullable JsExpression toSetTo) {
        boolean isGetter = toSetTo == null;
        TranslationContext context = context();
        ResolvedCall<FunctionDescriptor> resolvedCall = BindingUtils.getResolvedCallForArrayAccess(bindingContext(), expression, isGetter);
        if (!isGetter) {
            context = contextWithValueParameterAliasInArrayGetAccess(toSetTo);
        }
        return CallTranslator.INSTANCE$.translate(context, resolvedCall, arrayExpression);
    }

    @NotNull
    protected JsExpression getArrayExpression() {
        JetExpression arrayExpression = expression.getArrayExpression();
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

        JetExpression element = valueArgument.getArgumentExpression();
        return context().innerContextWithAliasesForExpressions(Collections.singletonMap(element, toSetTo));
    }

    @NotNull
    @Override
    public CachedAccessTranslator getCached() {
        List<TemporaryVariable> temporaries = new ArrayList<TemporaryVariable>();
        Map<JetExpression, JsExpression> aliases = new HashMap<JetExpression, JsExpression>();

        TemporaryVariable temporaryArrayExpression = context().declareTemporary(getArrayExpression());
        temporaries.add(temporaryArrayExpression);

        for (JetExpression jetExpression : expression.getIndexExpressions()) {
            JsExpression jsExpression = Translation.translateAsExpression(jetExpression, context());
            TemporaryVariable temporaryVariable = context().declareTemporary(jsExpression);
            temporaries.add(temporaryVariable);
            aliases.put(jetExpression, temporaryVariable.reference());
        }
        return new CachedArrayAccessTranslator(expression, context().innerContextWithAliasesForExpressions(aliases),
                                               temporaryArrayExpression, temporaries);
    }

    private static class CachedArrayAccessTranslator extends ArrayAccessTranslator implements CachedAccessTranslator {
        @NotNull
        private final TemporaryVariable temporaryArrayExpression;
        @NotNull
        private final List<TemporaryVariable> declaredTemporaries;

        protected CachedArrayAccessTranslator(
                @NotNull JetArrayAccessExpression expression,
                @NotNull TranslationContext context,
                @NotNull TemporaryVariable temporaryArrayExpression,
                @NotNull List<TemporaryVariable> temporaries
        ) {
            super(expression, context);
            this.temporaryArrayExpression = temporaryArrayExpression;
            declaredTemporaries = temporaries;
        }

        @NotNull
        @Override
        protected JsExpression getArrayExpression() {
            return temporaryArrayExpression.reference();
        }

        @NotNull
        @Override
        public List<TemporaryVariable> declaredTemporaries() {
            return declaredTemporaries;
        }
    }
}
