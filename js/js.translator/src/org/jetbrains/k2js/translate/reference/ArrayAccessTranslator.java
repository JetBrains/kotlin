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
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

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
        return translateAsGet(translateArrayExpression(), translateIndexExpressions());
    }

    @NotNull
    protected JsExpression translateAsGet(@NotNull JsExpression arrayExpression,
                                          @NotNull List<JsExpression> indexExpression) {
        return translateAsMethodCall(arrayExpression, indexExpression, /*isGetter = */ true);
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        return translateAsSet(translateArrayExpression(), translateIndexExpressions(), setTo);
    }

    @NotNull
    protected JsExpression translateAsSet(@NotNull JsExpression arrayExpression, @NotNull List<JsExpression> indexExpressions, @NotNull JsExpression toSetTo) {
        List<JsExpression> arguments = Lists.newArrayList(indexExpressions);
        arguments.add(toSetTo);
        return translateAsMethodCall(arrayExpression, arguments, /*isGetter = */ false);
    }

    @NotNull
    private JsExpression translateAsMethodCall(@NotNull JsExpression arrayExpression,
                                               @NotNull List<JsExpression> arguments,
                                               boolean isGetter) {
        return CallBuilder.build(context())
                .receiver(arrayExpression)
                .args(arguments)
                .resolvedCall(BindingUtils.getResolvedCallForArrayAccess(bindingContext(), expression, isGetter))
                .translate();
    }

    @NotNull
    protected List<JsExpression> translateIndexExpressions() {
        return TranslationUtils.translateExpressionList(context(), expression.getIndexExpressions());
    }

    @NotNull
    protected JsExpression translateArrayExpression() {
        JetExpression arrayExpression = expression.getArrayExpression();
        assert arrayExpression != null : "Code with parsing errors shouldn't be translated";
        return Translation.translateAsExpression(arrayExpression, context());
    }

    @NotNull
    @Override
    public CachedAccessTranslator getCached() {
        return new CachedArrayAccessTranslator(expression, context());
    }
}
