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
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.psi.*;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AccessTranslationUtils {
    private AccessTranslationUtils() {
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull KtExpression referenceExpression, @NotNull TranslationContext context) {
        return getAccessTranslator(referenceExpression, context, false);
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull KtExpression referenceExpression,
            @NotNull TranslationContext context, boolean forceOrderOfEvaluation) {
        referenceExpression = KtPsiUtil.deparenthesize(referenceExpression);
        assert referenceExpression != null;
        if (referenceExpression instanceof KtQualifiedExpression) {
            return QualifiedExpressionTranslator.getAccessTranslator((KtQualifiedExpression) referenceExpression, context, forceOrderOfEvaluation);
        }
        if (referenceExpression instanceof KtSimpleNameExpression) {
            return ReferenceTranslator.getAccessTranslator((KtSimpleNameExpression) referenceExpression, context);
        }
        if (referenceExpression instanceof KtArrayAccessExpression) {
            return getArrayAccessTranslator((KtArrayAccessExpression) referenceExpression, context, forceOrderOfEvaluation);
        }
        return new DefaultAccessTranslator(referenceExpression, context);
    }

    @NotNull
    private static AccessTranslator getArrayAccessTranslator(
            @NotNull KtArrayAccessExpression expression,
            @NotNull TranslationContext context,
            boolean forceOrderOfEvaluation
    ) {
        TranslationContext accessArrayContext;
        if (forceOrderOfEvaluation) {
            Map<KtExpression, JsExpression> indexesMap = new LinkedHashMap<KtExpression, JsExpression>();
            for(KtExpression indexExpression : expression.getIndexExpressions()) {
                JsExpression jsIndexExpression = context.cacheExpressionIfNeeded(
                        Translation.translateAsExpression(indexExpression, context));
                indexesMap.put(indexExpression, jsIndexExpression);
            }
            accessArrayContext = context.innerContextWithAliasesForExpressions(indexesMap);
        } else {
            accessArrayContext = context;
        }

        return ArrayAccessTranslator.newInstance(expression, accessArrayContext);
    }

    @NotNull
    public static JsExpression translateAsGet(@NotNull KtExpression expression, @NotNull TranslationContext context) {
        return (getAccessTranslator(expression, context)).translateAsGet();
    }
}
