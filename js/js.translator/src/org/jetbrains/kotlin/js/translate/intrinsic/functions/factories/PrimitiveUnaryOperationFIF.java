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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.patterns.DescriptorPredicate;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed;
import org.jetbrains.kotlin.js.translate.operation.OperatorTable;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.List;
import java.util.function.Predicate;

import static org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern;

public enum PrimitiveUnaryOperationFIF implements FunctionIntrinsicFactory {

    INSTANCE;

    private static final NamePredicate UNARY_OPERATIONS = new NamePredicate(OperatorNameConventions.UNARY_OPERATION_NAMES);
    @NotNull
    private static final DescriptorPredicate UNARY_OPERATION_FOR_PRIMITIVE_NUMBER =
            pattern(NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS, UNARY_OPERATIONS);
    @NotNull
    private static final Predicate<FunctionDescriptor> PRIMITIVE_UNARY_OPERATION_NAMES =
            UNARY_OPERATION_FOR_PRIMITIVE_NUMBER.or(pattern("Boolean.not")).or(pattern("Int|Short|Byte.inv"));
    @NotNull
    private static final DescriptorPredicate NO_PARAMETERS = descriptor -> !JsDescriptorUtils.hasParameters(descriptor);
    @NotNull
    private static final Predicate<FunctionDescriptor> PATTERN = PRIMITIVE_UNARY_OPERATION_NAMES.and(NO_PARAMETERS);

    private static final DescriptorPredicate INC_OPERATION_FOR_INT = pattern("Int.inc");
    private static final DescriptorPredicate DEC_OPERATION_FOR_INT = pattern("Int.dec");
    private static final DescriptorPredicate INC_OPERATION_FOR_BYTE = pattern("Byte.inc");
    private static final DescriptorPredicate DEC_OPERATION_FOR_BYTE = pattern("Byte.dec");
    private static final DescriptorPredicate INC_OPERATION_FOR_SHORT = pattern("Short.inc");
    private static final DescriptorPredicate DEC_OPERATION_FOR_SHORT = pattern("Short.dec");

    @NotNull
    private static final DescriptorPredicate INC_OPERATION_FOR_PRIMITIVE_NUMBER = pattern("Float|Double.inc()");

    @NotNull
    private static final DescriptorPredicate DEC_OPERATION_FOR_PRIMITIVE_NUMBER = pattern("Float|Double.dec()");

    private static class IntOverflowIntrinsic extends FunctionIntrinsicWithReceiverComputed {
        private final FunctionIntrinsicWithReceiverComputed underlyingIntrinsic;

        public IntOverflowIntrinsic(FunctionIntrinsicWithReceiverComputed underlyingIntrinsic) {
            this.underlyingIntrinsic = underlyingIntrinsic;
        }

        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            return JsAstUtils.toInt32(underlyingIntrinsic.apply(receiver, arguments, context));
        }
    }

    private static class ShortOverflowIntrinsic extends FunctionIntrinsicWithReceiverComputed {
        private final FunctionIntrinsicWithReceiverComputed underlyingIntrinsic;

        public ShortOverflowIntrinsic(FunctionIntrinsicWithReceiverComputed underlyingIntrinsic) {
            this.underlyingIntrinsic = underlyingIntrinsic;
        }

        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            return JsAstUtils.toShort(underlyingIntrinsic.apply(receiver, arguments, context));
        }
    }

    private static class ByteOverflowIntrinsic extends FunctionIntrinsicWithReceiverComputed {
        private final FunctionIntrinsicWithReceiverComputed underlyingIntrinsic;

        public ByteOverflowIntrinsic(FunctionIntrinsicWithReceiverComputed underlyingIntrinsic) {
            this.underlyingIntrinsic = underlyingIntrinsic;
        }

        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            return JsAstUtils.toByte(underlyingIntrinsic.apply(receiver, arguments, context));
        }
    }

    @NotNull
    private static final FunctionIntrinsicWithReceiverComputed NUMBER_INC_INTRINSIC = new FunctionIntrinsicWithReceiverComputed() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            assert arguments.size() == 0;
            return new JsBinaryOperation(JsBinaryOperator.ADD, receiver, context.program().getNumberLiteral(1));
        }
    };

    @NotNull
    private static final FunctionIntrinsicWithReceiverComputed NUMBER_DEC_INTRINSIC = new FunctionIntrinsicWithReceiverComputed() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            assert arguments.size() == 0;
            return new JsBinaryOperation(JsBinaryOperator.SUB, receiver, context.program().getNumberLiteral(1));
        }
    };

    private static abstract class UnaryOperationInstrinsicBase extends FunctionIntrinsicWithReceiverComputed {
        @NotNull
        public abstract JsExpression doApply(@NotNull JsExpression receiver, @NotNull TranslationContext context);

        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            assert arguments.size() == 0;
            return doApply(receiver, context);
        }
    }

    @NotNull
    private static final FunctionIntrinsic CHAR_PLUS = new UnaryOperationInstrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(
                @NotNull JsExpression receiver, @NotNull TranslationContext context
        ) {
            return JsAstUtils.charToInt(receiver);
        }
    };

    @NotNull
    private static final FunctionIntrinsic CHAR_MINUS = new UnaryOperationInstrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(
                @NotNull JsExpression receiver, @NotNull TranslationContext context
        ) {
            return new JsPrefixOperation(JsUnaryOperator.NEG, JsAstUtils.charToInt(receiver));
        }
    };

    @NotNull
    private static final FunctionIntrinsic CHAR_INC = new UnaryOperationInstrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(
                @NotNull JsExpression receiver, @NotNull TranslationContext context
        ) {
            return JsAstUtils.invokeKotlinFunction("charInc", receiver);
        }
    };

    @NotNull
    private static final FunctionIntrinsic CHAR_DEC = new UnaryOperationInstrinsicBase() {
        @NotNull
        @Override
        public JsExpression doApply(
                @NotNull JsExpression receiver, @NotNull TranslationContext context
        ) {
            return JsAstUtils.invokeKotlinFunction("charDec", receiver);
        }
    };

    @Nullable
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        if (!PATTERN.test(descriptor)) {
            return null;
        }

        if (pattern("Char.unaryPlus()").test(descriptor)) {
            return CHAR_PLUS;
        }
        if (pattern("Char.unaryMinus()").test(descriptor)) {
            return CHAR_MINUS;
        }
        if (pattern("Char.plus()").test(descriptor)) {
            return CHAR_PLUS;
        }
        if (pattern("Char.minus()").test(descriptor)) {
            return CHAR_MINUS;
        }
        if (pattern("Char.inc()").test(descriptor)) {
            return CHAR_INC;
        }
        if (pattern("Char.dec()").test(descriptor)) {
            return CHAR_DEC;
        }

        if (INC_OPERATION_FOR_INT.test(descriptor)) {
            return new IntOverflowIntrinsic(NUMBER_INC_INTRINSIC);
        }
        if (DEC_OPERATION_FOR_INT.test(descriptor)) {
            return new IntOverflowIntrinsic(NUMBER_DEC_INTRINSIC);
        }
        if (INC_OPERATION_FOR_SHORT.test(descriptor)) {
            return new ShortOverflowIntrinsic(NUMBER_INC_INTRINSIC);
        }
        if (DEC_OPERATION_FOR_SHORT.test(descriptor)) {
            return new ShortOverflowIntrinsic(NUMBER_DEC_INTRINSIC);
        }
        if (INC_OPERATION_FOR_BYTE.test(descriptor)) {
            return new ByteOverflowIntrinsic(NUMBER_INC_INTRINSIC);
        }
        if (DEC_OPERATION_FOR_BYTE.test(descriptor)) {
            return new ByteOverflowIntrinsic(NUMBER_DEC_INTRINSIC);
        }

        if (INC_OPERATION_FOR_PRIMITIVE_NUMBER.test(descriptor)) {
            return NUMBER_INC_INTRINSIC;
        }
        if (DEC_OPERATION_FOR_PRIMITIVE_NUMBER.test(descriptor)) {
            return NUMBER_DEC_INTRINSIC;
        }


        Name name = descriptor.getName();

        JsUnaryOperator jsOperator;
        if ("inv".equals(name.asString())) {
            jsOperator = JsUnaryOperator.BIT_NOT;
        }
        else {
            KtToken jetToken = OperatorConventions.UNARY_OPERATION_NAMES.inverse().get(name);
            jsOperator = OperatorTable.getUnaryOperator(jetToken);
        }

        JsUnaryOperator finalJsOperator = jsOperator;
        return new FunctionIntrinsicWithReceiverComputed() {
            @NotNull
            @Override
            public JsExpression apply(@Nullable JsExpression receiver,
                    @NotNull List<? extends JsExpression> arguments,
                    @NotNull TranslationContext context) {
                assert receiver != null;
                assert arguments.size() == 0 : "Unary operator should not have arguments.";
                //NOTE: cannot use this for increment/decrement
                return new JsPrefixOperation(finalJsOperator, receiver);
            }
        };
    }
}
