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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;

import java.util.List;

public class KotlinFunctionIntrinsic extends FunctionIntrinsic {
    @NotNull
    private final JsNameRef function;

    public KotlinFunctionIntrinsic(@NotNull String functionName) {
        function = new JsNameRef(functionName, Namer.KOTLIN_NAME);
    }

    @NotNull
    @Override
    public JsExpression apply(
            @Nullable JsExpression receiver,
            @NotNull List<JsExpression> arguments,
            @NotNull TranslationContext context
    ) {
        return new JsInvocation(function, receiver == null ? arguments : TranslationUtils.generateInvocationArguments(receiver, arguments));
    }
}
