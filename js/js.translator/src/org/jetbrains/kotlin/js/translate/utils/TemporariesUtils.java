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

package org.jetbrains.kotlin.js.translate.utils;

import com.google.common.collect.Lists;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;

import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.newSequence;

//TODO: look for what should go there
public final class TemporariesUtils {
    private TemporariesUtils() {
    }

    @NotNull
    public static List<TemporaryVariable> fromExpressionList(@NotNull List<JsExpression> expressions,
                                                             @NotNull TranslationContext context) {
        List<TemporaryVariable> result = Lists.newArrayList();
        for (JsExpression expression : expressions) {
            result.add(context.declareTemporary(expression));
        }
        return result;
    }

    @NotNull
    public static List<JsExpression> toExpressionList(@NotNull List<TemporaryVariable> temporaries) {
        List<JsExpression> result = Lists.newArrayList();
        for (TemporaryVariable temp : temporaries) {
            result.add(temp.reference());
        }
        return result;
    }

    @NotNull
    public static JsExpression temporariesInitialization(@NotNull TemporaryVariable... temporaries) {
        List<JsExpression> result = Lists.newArrayList();
        for (TemporaryVariable temporary : temporaries) {
            result.add(temporary.assignmentExpression());
        }
        return newSequence(result);
    }

    @NotNull
    public static List<JsExpression> temporariesInitialization(@NotNull List<TemporaryVariable> temporaries) {
        List<JsExpression> result = Lists.newArrayList();
        for (TemporaryVariable temporary : temporaries) {
            result.add(temporary.assignmentExpression());
        }
        return result;
    }
}
