/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsInvocation;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;

import java.util.ArrayList;
import java.util.List;

public class KotlinFunctionIntrinsic extends FunctionIntrinsicWithReceiverComputed {
    @NotNull
    private final String functionName;
    private final JsExpression[] additionalArguments;

    public KotlinFunctionIntrinsic(@NotNull String functionName, JsExpression... additionalArguments) {
        this.functionName = functionName;
        this.additionalArguments = additionalArguments;
    }

    @NotNull
    @Override
    public JsExpression apply(
            @Nullable JsExpression receiver,
            @NotNull List<? extends JsExpression> arguments,
            @NotNull TranslationContext context
    ) {
        JsExpression function = JsAstUtils.pureFqn(functionName, Namer.kotlinObject());
        if (additionalArguments.length > 0) {
            List<JsExpression> newArguments = new ArrayList<>(arguments);
            for (JsExpression e : additionalArguments) {
                newArguments.add(e.deepCopy());
            }
            arguments = newArguments;
        }
        return new JsInvocation(function, receiver == null ? arguments : TranslationUtils.generateInvocationArguments(receiver, arguments));
    }
}
