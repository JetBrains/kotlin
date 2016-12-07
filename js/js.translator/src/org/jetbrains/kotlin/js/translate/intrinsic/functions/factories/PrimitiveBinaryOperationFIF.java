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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.patterns.DescriptorPredicate;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.RangeToIntrinsic;
import org.jetbrains.kotlin.js.translate.operation.OperatorTable;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.List;

import static org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern;

public enum PrimitiveBinaryOperationFIF implements FunctionIntrinsicFactory {
    INSTANCE;

    private static abstract class BinaryOperationIntrinsicBase extends FunctionIntrinsic {
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
    private static final BinaryOperationIntrinsicBase INTEGER_DIVISION_INTRINSIC = new BinaryOperationIntrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            JsBinaryOperation div = new JsBinaryOperation(JsBinaryOperator.DIV, left, right);
            return JsAstUtils.toInt32(div);
        }
    };

    @NotNull
    private static final BinaryOperationIntrinsicBase BUILTINS_COMPARE_TO_INTRINSIC = new BinaryOperationIntrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            return JsAstUtils.compareTo(left, right);
        }
    };

    @NotNull
    private static final BinaryOperationIntrinsicBase PRIMITIVE_NUMBER_COMPARE_TO_INTRINSIC = new BinaryOperationIntrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            return JsAstUtils.primitiveCompareTo(left, right);
        }
    };

    @NotNull
    private static final NamePredicate BINARY_OPERATIONS = new NamePredicate(OperatorNameConventions.BINARY_OPERATION_NAMES);
    private static final DescriptorPredicate PRIMITIVE_NUMBERS_BINARY_OPERATIONS =
            pattern(NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS, BINARY_OPERATIONS);

    private static final DescriptorPredicate PRIMITIVE_NUMBERS_COMPARE_TO_OPERATIONS =
            pattern(NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS, "compareTo");
    private static final Predicate<FunctionDescriptor> INT_WITH_BIT_OPERATIONS = Predicates.or(
            pattern("Int.or|and|xor|shl|shr|ushr"),
            pattern("Short|Byte.or|and|xor")
    );
    private static final DescriptorPredicate BOOLEAN_OPERATIONS = pattern("Boolean.or|and|xor");
    private static final DescriptorPredicate STRING_PLUS = pattern("String.plus");

    private static final DescriptorPredicate CHAR_RANGE_TO = pattern("Char.rangeTo(Char)");
    private static final DescriptorPredicate NUMBER_RANGE_TO = pattern("Byte|Short|Int.rangeTo(Byte|Short|Int)");

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
        if (CHAR_RANGE_TO.apply(descriptor)) {
            return new RangeToIntrinsic(descriptor);
        }

        if (PRIMITIVE_NUMBERS_COMPARE_TO_OPERATIONS.apply(descriptor)) {
            return PRIMITIVE_NUMBER_COMPARE_TO_INTRINSIC;
        }


        if (KotlinBuiltIns.isBuiltIn(descriptor) && descriptor.getName().equals(OperatorNameConventions.COMPARE_TO)) {
            return BUILTINS_COMPARE_TO_INTRINSIC;
        }

        if (!PREDICATE.apply(descriptor)) {
            return null;
        }


        if (pattern("Int|Short|Byte.div(Int|Short|Byte)").apply(descriptor)) {
            return INTEGER_DIVISION_INTRINSIC;
        }
        if (NUMBER_RANGE_TO.apply(descriptor)) {
            return new RangeToIntrinsic(descriptor);
        }
        if (INT_WITH_BIT_OPERATIONS.apply(descriptor)) {
            JsBinaryOperator op = BINARY_BITWISE_OPERATIONS.get(descriptor.getName().asString());
            if (op != null) {
                return new PrimitiveBinaryOperationFunctionIntrinsic(op);
            }
        }
        JsBinaryOperator operator = getOperator(descriptor);
        BinaryOperationIntrinsicBase result = new PrimitiveBinaryOperationFunctionIntrinsic(operator);

        if (pattern("Char.plus|minus(Int)").apply(descriptor)) {
            return new CharAndIntBinaryOperationFunctionIntrinsic(result);
        }
        if (pattern("Char.minus(Char)").apply(descriptor)) {
            return new CharAndCharBinaryOperationFunctionIntrinsic(result);
        }
        return result;
    }

    @NotNull
    private static JsBinaryOperator getOperator(@NotNull FunctionDescriptor descriptor) {
        // Temporary hack to get '%' for deprecated 'mod' operator
        Name descriptorName = descriptor.getName().equals(OperatorNameConventions.MOD) ? OperatorNameConventions.REM : descriptor.getName();

        KtToken token = OperatorConventions.BINARY_OPERATION_NAMES.inverse().get(descriptorName);
        if (token == null) {
            token = OperatorConventions.BOOLEAN_OPERATIONS.inverse().get(descriptorName);
        }
        if (token == null) {
            assert descriptorName.asString().equals("xor");
            return JsBinaryOperator.BIT_XOR;
        }
        return OperatorTable.getBinaryOperator(token);
    }

    private static class PrimitiveBinaryOperationFunctionIntrinsic extends BinaryOperationIntrinsicBase {

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

    private static class CharAndIntBinaryOperationFunctionIntrinsic extends BinaryOperationIntrinsicBase {

        @NotNull
        private final BinaryOperationIntrinsicBase functionIntrinsic;

        private CharAndIntBinaryOperationFunctionIntrinsic(@NotNull BinaryOperationIntrinsicBase functionIntrinsic) {
            this.functionIntrinsic = functionIntrinsic;
        }

        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            return JsAstUtils.toChar(functionIntrinsic.doApply(JsAstUtils.charToInt(left), right, context));
        }
    }

    private static class CharAndCharBinaryOperationFunctionIntrinsic extends BinaryOperationIntrinsicBase {

        @NotNull
        private final BinaryOperationIntrinsicBase functionIntrinsic;

        private CharAndCharBinaryOperationFunctionIntrinsic(@NotNull BinaryOperationIntrinsicBase functionIntrinsic) {
            this.functionIntrinsic = functionIntrinsic;
        }

        @NotNull
        @Override
        public JsExpression doApply(@NotNull JsExpression left, @NotNull JsExpression right, @NotNull TranslationContext context) {
            return functionIntrinsic.doApply(JsAstUtils.charToInt(left), JsAstUtils.charToInt(right), context);
        }
    }
}
