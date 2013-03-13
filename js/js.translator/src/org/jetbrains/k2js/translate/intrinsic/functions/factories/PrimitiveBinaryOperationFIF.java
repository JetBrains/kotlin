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
import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;
import org.jetbrains.k2js.translate.operation.OperatorTable;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.factories.NumberConversionFIF.INTEGER_NUMBER_TYPES;
import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;

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
            JsNameRef expr = AstUtil.newQualifiedNameRef("Kotlin.NumberRange");
            HasArguments numberRangeConstructorInvocation = context.isEcma5() ? new JsInvocation(expr) : new JsNew(expr);
            //TODO: add tests and correct expression for reversed ranges.
            setArguments(numberRangeConstructorInvocation, rangeStart, rangeEnd);
            return (JsExpression) numberRangeConstructorInvocation;
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
            TemporaryVariable left = context.declareTemporary(receiver);
            assert arguments.size() == 1;
            TemporaryVariable right = context.declareTemporary(arguments.get(0));
            JsBinaryOperation divRes = new JsBinaryOperation(JsBinaryOperator.DIV, left.reference(), right.reference());
            JsBinaryOperation modRes = new JsBinaryOperation(JsBinaryOperator.MOD, left.reference(), right.reference());
            JsBinaryOperation fractionalPart = new JsBinaryOperation(JsBinaryOperator.DIV, modRes, right.reference());
            return AstUtil.newSequence(left.assignmentExpression(), right.assignmentExpression(), subtract(divRes, fractionalPart));
        }
    };

    @NotNull
    private static final NamePredicate BINARY_OPERATIONS = new NamePredicate(OperatorConventions.BINARY_OPERATION_NAMES.values());

    @NotNull
    @Override
    public Predicate<FunctionDescriptor> getPredicate() {
        //TODO: check that it is binary operation
        return Predicates.or(pattern(NamePredicate.PRIMITIVE_NUMBERS, BINARY_OPERATIONS),
                             pattern("Boolean.or|and|xor"),
                             pattern("String.plus"));
    }

    @NotNull
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        if (pattern(INTEGER_NUMBER_TYPES + ".div").apply(descriptor)) {
            return INTEGER_DIVISION_INTRINSIC;
        }
        if (descriptor.getName().equals(Name.identifier("rangeTo"))) {
            return RANGE_TO_INTRINSIC;
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
            assert descriptor.getName().getName().equals("xor");
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
