/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NameChecker;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.Pattern;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder;
import org.jetbrains.k2js.translate.operation.OperatorTable;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;

/**
 * @author Pavel Talanov
 */
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
            JsBinaryOperation rangeSize = sum(subtract(rangeEnd, rangeStart),
                                              context.program().getNumberLiteral(1));
            JsNameRef expr = AstUtil.newQualifiedNameRef("Kotlin.NumberRange");
            HasArguments numberRangeConstructorInvocation = context.isEcma5() ? AstUtil.newInvocation(expr) : new JsNew(expr);
            //TODO: add tests and correct expression for reversed ranges.
            JsBooleanLiteral isRangeReversed = context.program().getFalseLiteral();
            setArguments(numberRangeConstructorInvocation, rangeStart, rangeSize, isRangeReversed);
            return (JsExpression) numberRangeConstructorInvocation;
        }
    };
    @NotNull
    private static final NameChecker BINARY_OPERATIONS = new NameChecker(OperatorConventions.BINARY_OPERATION_NAMES.values());

    @NotNull
    @Override
    public Pattern getPattern() {
        //TODO: check that it is binary operation
        return PatternBuilder.any(pattern(NameChecker.PRIMITIVE_NUMBERS, BINARY_OPERATIONS),
                                  pattern("Boolean.or|and|xor"),
                                  pattern("String.plus"));
    }

    @NotNull
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        if (descriptor.getName().equals(Name.identifier("rangeTo"))) {
            return RANGE_TO_INTRINSIC;
        }
        final JsBinaryOperator operator = getOperator(descriptor);
        return new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(@Nullable JsExpression receiver,
                    @NotNull List<JsExpression> arguments,
                    @NotNull TranslationContext context) {
                assert arguments.size() == 1 : "Binary operator should have a receiver and one argument";
                return new JsBinaryOperation(operator, receiver, arguments.get(0));
            }
        };
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
}
