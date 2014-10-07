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
import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;
import org.jetbrains.k2js.translate.operation.OperatorTable;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;

public enum PrimitiveBinaryOperationFIF implements FunctionIntrinsicFactory {
    INSTANCE;

    private static abstract class BinaryOperationInstrinsicBase extends FunctionIntrinsic {
        @NotNull
        public abstract JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context);

        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.size() == 1;
            return doApply(receiver, arguments.get(0), context);
        }
    }

    @NotNull
    private static final BinaryOperationInstrinsicBase RANGE_TO_INTRINSIC = new BinaryOperationInstrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            //TODO: add tests and correct expression for reversed ranges.
            return JsAstUtils.numberRangeTo(left, right);
        }
    };

    @NotNull
    private static final BinaryOperationInstrinsicBase CHAR_RANGE_TO_INTRINSIC = new BinaryOperationInstrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            //TODO: add tests and correct expression for reversed ranges.
            return JsAstUtils.charRangeTo(left, right);
        }
    };

    @NotNull
    private static final BinaryOperationInstrinsicBase INTEGER_DIVISION_INTRINSIC = new BinaryOperationInstrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            JsBinaryOperation div = new JsBinaryOperation(JsBinaryOperator.DIV, left, right);
            return JsAstUtils.toInt32(div);
        }
    };

    @NotNull
    private static final BinaryOperationInstrinsicBase BUILTINS_COMPARE_TO_INTRINSIC = new BinaryOperationInstrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            return JsAstUtils.compareTo(left, right);
        }
    };

    @NotNull
    private static final BinaryOperationInstrinsicBase PRIMITIVE_NUMBER_COMPARE_TO_INTRINSIC = new BinaryOperationInstrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            return JsAstUtils.primitiveCompareTo(left, right);
        }
    };

    @NotNull
    private static final NamePredicate BINARY_OPERATIONS = new NamePredicate(OperatorConventions.BINARY_OPERATION_NAMES.values());
    private static final DescriptorPredicate PRIMITIVE_NUMBERS_BINARY_OPERATIONS =
            pattern(NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS, BINARY_OPERATIONS);

    private static final DescriptorPredicate PRIMITIVE_NUMBERS_COMPARE_TO_OPERATIONS =
            pattern(NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS, "compareTo");
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
                                                                                 STRING_PLUS, INT_WITH_BIT_OPERATIONS,
                                                                                 PRIMITIVE_NUMBERS_COMPARE_TO_OPERATIONS);

    @Nullable
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        if (pattern("Int|Short|Byte|Double|Float.compareTo(Char)").apply(descriptor)) {
            return new WithCharAsSecondOperandFunctionIntrinsic(PRIMITIVE_NUMBER_COMPARE_TO_INTRINSIC);
        }

        if (pattern("Char.compareTo(Int|Short|Byte|Double|Float)").apply(descriptor)) {
            return new WithCharAsFirstOperandFunctionIntrinsic(PRIMITIVE_NUMBER_COMPARE_TO_INTRINSIC);
        }

        if (pattern("Char.rangeTo(Char)").apply(descriptor)) {
            return CHAR_RANGE_TO_INTRINSIC;
        }

        if (PRIMITIVE_NUMBERS_COMPARE_TO_OPERATIONS.apply(descriptor)) {
            return PRIMITIVE_NUMBER_COMPARE_TO_INTRINSIC;
        }


        if (JsDescriptorUtils.isBuiltin(descriptor) && descriptor.getName().equals(OperatorConventions.COMPARE_TO)) {
            return BUILTINS_COMPARE_TO_INTRINSIC;
        }

        if (!PREDICATE.apply(descriptor)) {
            return null;
        }


        if (pattern("Int|Short|Byte.div(Int|Short|Byte)").apply(descriptor)) {
                return INTEGER_DIVISION_INTRINSIC;
        }
        if (pattern("Char.div(Int|Short|Byte)").apply(descriptor)) {
            return new WithCharAsFirstOperandFunctionIntrinsic(INTEGER_DIVISION_INTRINSIC);
        }
        if (pattern("Int|Short|Byte.div(Char)").apply(descriptor)) {
            return new WithCharAsSecondOperandFunctionIntrinsic(INTEGER_DIVISION_INTRINSIC);
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
        BinaryOperationInstrinsicBase result = new PrimitiveBinaryOperationFunctionIntrinsic(operator);

        if (pattern("Char.plus|minus|times|div|mod(Int|Short|Byte|Double|Float)").apply(descriptor)) {
            return new WithCharAsFirstOperandFunctionIntrinsic(result);
        }
        if (pattern("Int|Short|Byte|Double|Float.plus|minus|times|div|mod(Char)").apply(descriptor)) {
            return new WithCharAsSecondOperandFunctionIntrinsic(result);
        }
        return result;
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

    private static class PrimitiveBinaryOperationFunctionIntrinsic extends BinaryOperationInstrinsicBase {

        @NotNull
        private final JsBinaryOperator operator;

        private PrimitiveBinaryOperationFunctionIntrinsic(@NotNull JsBinaryOperator operator) {
            this.operator = operator;
        }

        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            return new JsBinaryOperation(operator, left, right);
        }
    }

    private static class WithCharAsFirstOperandFunctionIntrinsic extends BinaryOperationInstrinsicBase {

        @NotNull
        private final BinaryOperationInstrinsicBase functionIntrinsic;

        private WithCharAsFirstOperandFunctionIntrinsic(@NotNull BinaryOperationInstrinsicBase functionIntrinsic) {
            this.functionIntrinsic = functionIntrinsic;
        }

        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            return functionIntrinsic.doApply(JsAstUtils.charToInt(left), right, context);
        }
    }

    private static class WithCharAsSecondOperandFunctionIntrinsic extends BinaryOperationInstrinsicBase {

        @NotNull
        private final BinaryOperationInstrinsicBase functionIntrinsic;

        private WithCharAsSecondOperandFunctionIntrinsic(@NotNull BinaryOperationInstrinsicBase functionIntrinsic) {
            this.functionIntrinsic = functionIntrinsic;
        }

        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            return functionIntrinsic.doApply(left, JsAstUtils.charToInt(right), context);
        }
    }
}
