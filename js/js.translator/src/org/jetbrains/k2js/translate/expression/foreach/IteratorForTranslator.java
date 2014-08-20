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

package org.jetbrains.k2js.translate.expression.foreach;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.k2js.translate.callTranslator.CallTranslator;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;

import static org.jetbrains.k2js.translate.utils.BindingUtils.*;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange;

public final class IteratorForTranslator extends ForTranslator {
    @NotNull
    private final Pair<JsVars.JsVar, JsExpression> iterator;

    @NotNull
    public static JsStatement doTranslate(@NotNull JetForExpression expression,
            @NotNull TranslationContext context) {
        return (new IteratorForTranslator(expression, context).translate());
    }

    private IteratorForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(forExpression, context);
        iterator = context.dynamicContext().createTemporary(iteratorMethodInvocation());
    }

    @NotNull
    private JsBlock translate() {
        return new JsBlock(new JsVars(iterator.first), new JsWhile(hasNextMethodInvocation(), translateBody(nextMethodInvocation())));
    }

    @NotNull
    private JsExpression nextMethodInvocation() {
        return translateMethodInvocation(iterator.second, getNextFunction(bindingContext(), getLoopRange(expression)));
    }

    @NotNull
    private JsExpression hasNextMethodInvocation() {
        ResolvedCall<FunctionDescriptor> resolvedCall = getHasNextCallable(bindingContext(), getLoopRange(expression));
        return translateMethodInvocation(iterator.second, resolvedCall);
    }

    @NotNull
    private JsExpression iteratorMethodInvocation() {
        JetExpression rangeExpression = getLoopRange(expression);
        JsExpression range = Translation.translateAsExpression(rangeExpression, context());
        ResolvedCall<FunctionDescriptor> resolvedCall = getIteratorFunction(bindingContext(), rangeExpression);
        return translateMethodInvocation(range, resolvedCall);
    }

    @NotNull
    private JsExpression translateMethodInvocation(@Nullable JsExpression receiver,
            @NotNull ResolvedCall<FunctionDescriptor> resolvedCall) {
        return CallTranslator.INSTANCE$.translate(context(), resolvedCall, receiver);
    }
}
