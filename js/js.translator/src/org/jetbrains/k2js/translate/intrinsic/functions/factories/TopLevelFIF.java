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

package org.jetbrains.k2js.translate.intrinsic.functions.factories;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.BuiltInFunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.CallStandardMethodIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;

public final class TopLevelFIF extends CompositeFIF {
    @NotNull
    public static final CallStandardMethodIntrinsic EQUALS = new CallStandardMethodIntrinsic(new JsNameRef("equals", "Kotlin"), true, 1);
    @NotNull
    private static final FunctionIntrinsic RETURN_RECEIVER_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            return receiver;
        }
    };
    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new TopLevelFIF();

    private TopLevelFIF() {
        add(pattern("toString"), new BuiltInFunctionIntrinsic("toString"));
        add(pattern("equals"), EQUALS);
        add(pattern(NamePredicate.PRIMITIVE_NUMBERS, "equals"), EQUALS);
        add(pattern("String|Boolean|Char|Number.equals"), EQUALS);
        add(pattern("arrayOfNulls"), new CallStandardMethodIntrinsic(new JsNameRef("nullArray", "Kotlin"), false, 1));
        add(pattern("iterator"), RETURN_RECEIVER_INTRINSIC);

        add(PatternBuilder.create("java", "util", "set").receiverParameterExists(true), new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(
                    @Nullable JsExpression receiver, @NotNull List<JsExpression> arguments, @NotNull TranslationContext context
            ) {
                return new JsInvocation(new JsNameRef("put", receiver), arguments);
            }
        });
    }
}
