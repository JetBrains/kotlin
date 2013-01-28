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
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.TemporariesUtils.fromExpressionList;
import static org.jetbrains.k2js.translate.utils.TemporariesUtils.toExpressionList;

public final class CachedArrayAccessTranslator extends ArrayAccessTranslator implements CachedAccessTranslator {

    @NotNull
    private final TemporaryVariable arrayExpression;
    @NotNull
    private final List<TemporaryVariable> indexExpressions;

    /*package*/ CachedArrayAccessTranslator(@NotNull JetArrayAccessExpression expression,
                                            @NotNull TranslationContext context) {
        super(expression, context);
        arrayExpression = context.declareTemporary(translateArrayExpression());
        indexExpressions = fromExpressionList(translateIndexExpressions(), context);
    }

    @NotNull
    @Override
    public JsExpression translateAsGet() {
        return translateAsGet(arrayExpression.reference(), toExpressionList(indexExpressions));
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        return translateAsSet(arrayExpression.reference(), toExpressionList(indexExpressions), setTo);
    }

    @NotNull
    @Override
    public List<TemporaryVariable> declaredTemporaries() {
        List<TemporaryVariable> result = Lists.newArrayList();
        result.add(arrayExpression);
        result.addAll(indexExpressions);
        return result;
    }
}
