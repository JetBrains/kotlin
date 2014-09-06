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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;

//TODO: support longs and chars
public final class NumberConversionFIF extends CompositeFIF {

    @NotNull
    private static final FunctionIntrinsic RETURN_RECEIVER = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            assert arguments.isEmpty();
            return receiver;
        }
    };

    @NotNull
    public static final String INTEGER_NUMBER_TYPES = "Int|Byte|Short";

    @NotNull
    private static final FunctionIntrinsic TO_INT32 = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            assert arguments.isEmpty();
            return JsAstUtils.toInt32(receiver, context);
        }
    };

    @NotNull
    private static final FunctionIntrinsic TO_SHORT = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            assert arguments.isEmpty();
            return JsAstUtils.toShort(receiver);
        }
    };

    @NotNull
    private static final FunctionIntrinsic TO_BYTE = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            assert arguments.isEmpty();
            return JsAstUtils.toByte(receiver);
        }
    };

    @NotNull
    private static final Predicate<FunctionDescriptor> returnReceiverPredicate = Predicates.or(
            pattern("Int.toInt|toFloat|toDouble"),
            pattern("Short.toShort|toInt|toFloat|toDouble"),
            pattern("Byte.toByte|toShort|toInt|toFloat|toDouble"),
            pattern("Float|Double|Number.toFloat|toDouble")
    );

    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new NumberConversionFIF();

    //NOTE: treat Number as if it is floating point type
    private NumberConversionFIF() {
        add(returnReceiverPredicate, RETURN_RECEIVER);
        add(pattern("Float|Double|Number.toInt"), TO_INT32);
        add(pattern("Int|Float|Double|Number.toShort"), TO_SHORT);
        add(pattern("Short|Int|Float|Double|Number.toByte"), TO_BYTE);
    }
}