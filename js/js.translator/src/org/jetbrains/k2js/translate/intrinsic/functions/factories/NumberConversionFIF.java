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

import com.google.common.collect.Sets;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;

import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.*;
import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.subtract;

public final class NumberConversionFIF extends CompositeFIF {
    @NotNull
    private static final NamePredicate SUPPORTED_CONVERSIONS;

    static {
        Set<Name> supportedConversions = Sets.newHashSet(NUMBER_CONVERSIONS);
        //TODO: support longs and chars
        supportedConversions.remove(CHAR);
        supportedConversions.remove(LONG);
        SUPPORTED_CONVERSIONS = new NamePredicate(supportedConversions);
    }

    @NotNull
    private static final NamePredicate FLOATING_POINT_CONVERSIONS = new NamePredicate(FLOAT, DOUBLE);

    @NotNull
    private static final NamePredicate INTEGER_CONVERSIONS = new NamePredicate(INT, SHORT, BYTE);

    @NotNull
    private static final FunctionIntrinsic RETURN_RECEIVER = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.isEmpty();
            return receiver;
        }
    };

    @NotNull
    public static final String INTEGER_NUMBER_TYPES = "Int|Byte|Short";
    //NOTE: treat Number as if it is floating point type
    @NotNull
    private static final String FLOATING_POINT_NUMBER_TYPES = "Float|Double|Number";
    @NotNull
    private static final FunctionIntrinsic GET_INTEGER_PART = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.isEmpty();
            JsNameRef toConvertReference = context.declareTemporary(null).reference();
            JsBinaryOperation fractional =
                    new JsBinaryOperation(JsBinaryOperator.MOD, toConvertReference, context.program().getNumberLiteral(1));
            return subtract(assignment(toConvertReference, receiver), fractional);
        }
    };
    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new NumberConversionFIF();

    private NumberConversionFIF() {
        add(pattern(INTEGER_NUMBER_TYPES, SUPPORTED_CONVERSIONS), RETURN_RECEIVER);
        add(pattern(FLOATING_POINT_NUMBER_TYPES, INTEGER_CONVERSIONS), GET_INTEGER_PART);
        add(pattern(FLOATING_POINT_NUMBER_TYPES, FLOATING_POINT_CONVERSIONS), RETURN_RECEIVER);
    }
}
