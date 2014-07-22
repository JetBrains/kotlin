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
import com.google.common.collect.ImmutableMap;
import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;
import org.jetbrains.k2js.translate.operation.OperatorTable;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.factories.NumberConversionFIF.INTEGER_NUMBER_TYPES;
import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.setArguments;

public enum PrimitiveBinaryOperationFIF implements FunctionIntrinsicFactory {
    INSTANCE;

    @NotNull
    private static final FunctionIntrinsic RANGE_TO_INTRINSIC = new FunctionIntrinsic() {

        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression rangeStart, @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert arguments.size() == 1 : "RangeTo must have one argument.";
            assert rangeStart != null;
            JsExpression rangeEnd = arguments.get(0);
            JsNameRef expr = new JsNameRef("NumberRange", "Kotlin");
            HasArguments numberRangeConstructorInvocation = new JsNew(expr);
            //TODO: add tests and correct expression for reversed ranges.
            setArguments(numberRangeConstructorInvocation, rangeStart, rangeEnd);
            return numberRangeConstructorInvocation;
        }
    };

    @NotNull
    private static final FunctionIntrinsic INTEGER_DIVISION_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.size() == 1;
            JsBinaryOperation div = new JsBinaryOperation(JsBinaryOperator.DIV, receiver, arguments.get(0));
            JsBinaryOperation toInt32 = new JsBinaryOperation(JsBinaryOperator.BIT_OR, div, context.program().getNumberLiteral(0));
            return toInt32;
        }
    };

    @NotNull
    private static final NamePredicate BINARY_OPERATIONS = new NamePredicate(OperatorConventions.BINARY_OPERATION_NAMES.values());
    private static final DescriptorPredicate PRIMITIVE_NUMBERS_BINARY_OPERATIONS = pattern(NamePredicate.PRIMITIVE_NUMBERS, BINARY_OPERATIONS);
    private static final DescriptorPredicate INT_WITH_BIT_OPERATIONS = pattern("Int.or|and|xor|shl|shr|ushr");
    private static final DescriptorPredicate BOOLEAN_OPERATIONS = pattern("Boolean.or|and|xor");
    private static final DescriptorPredicate STRING_PLUS = pattern("String.plus");

    private static final ImmutableMap<String, JsBinaryOperator> BINARY_BITWISE_OPERATIONS = ImmutableMap.<String, JsBinaryOperator>builder()
            .put("or", JsBinaryOperator.BIT_OR)
            .put("and", JsBinaryOperator.BIT_AND)
            .put("xor", JsBinaryOperator.BIT_XOR)
            .put("shl", JsBinaryOperator.SHL)
            .put("shr", JsBinaryOperator.SHR)
            .put("ushr", JsBinaryOperator.SHRU)
            .build();

    private static final Predicate<FunctionDescriptor> PREDICATE = Predicates.or(PRIMITIVE_NUMBERS_BINARY_OPERATIONS, BOOLEAN_OPERATIONS,
                                                                                 STRING_PLUS, INT_WITH_BIT_OPERATIONS);

    @Nullable
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        if (!PREDICATE.apply(descriptor)) {
            return null;
        }

        if (pattern(INTEGER_NUMBER_TYPES + ".div").apply(descriptor)) {
            JetType resultType = descriptor.getReturnType();
            if (!KotlinBuiltIns.getInstance().getFloatType().equals(resultType) &&
                !KotlinBuiltIns.getInstance().getDoubleType().equals(resultType))
                return INTEGER_DIVISION_INTRINSIC;
        }
        if (descriptor.getName().equals(Name.identifier("rangeTo"))) {
            return RANGE_TO_INTRINSIC;
        }
        if (INT_WITH_BIT_OPERATIONS.apply(descriptor)) {
            JsBinaryOperator op = BINARY_BITWISE_OPERATIONS.get(descriptor.getName().asString());
            if (op != null) {
                return new PrimitiveBinaryOperationFunctionIntrinsic(op);
            }
        }
        JsBinaryOperator operator = getOperator(descriptor);
        return new PrimitiveBinaryOperationFunctionIntrinsic(operator);
    }

    @NotNull
    private static JsBinaryOperator getOperator(@NotNull FunctionDescriptor descriptor) {
        JetToken token = OperatorConventions.BINARY_OPERATION_NAMES.inverse().get(descriptor.getName());
        if (token == null) {
            token = OperatorConventions.BOOLEAN_OPERATIONS.inverse().get(descriptor.getName());
        }
        if (token == null) {
            assert descriptor.getName().asString().equals("xor");
            return JsBinaryOperator.BIT_XOR;
        }
        return OperatorTable.getBinaryOperator(token);
    }

    private static class PrimitiveBinaryOperationFunctionIntrinsic extends FunctionIntrinsic {

        @NotNull
        private final JsBinaryOperator operator;

        private PrimitiveBinaryOperationFunctionIntrinsic(@NotNull JsBinaryOperator operator) {
            this.operator = operator;
        }

        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.size() == 1 : "Binary operator should have a receiver and one argument";
            return new JsBinaryOperation(operator, receiver, arguments.get(0));
        }
    }
}
