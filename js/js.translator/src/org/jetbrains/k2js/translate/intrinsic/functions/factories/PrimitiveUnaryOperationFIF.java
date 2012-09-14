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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsPrefixOperation;
import com.google.dart.compiler.backend.js.ast.JsUnaryOperator;
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
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate.PRIMITIVE_NUMBERS;
import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;

public enum PrimitiveUnaryOperationFIF implements FunctionIntrinsicFactory {

    INSTANCE;

    private static final NamePredicate UNARY_OPERATIONS = new NamePredicate(OperatorConventions.UNARY_OPERATION_NAMES.values());
    @NotNull
    private static final DescriptorPredicate UNARY_OPERATION_FOR_PRIMITIVE_NUMBER =
            pattern(PRIMITIVE_NUMBERS, UNARY_OPERATIONS);
    @NotNull
    private static final Predicate<FunctionDescriptor> PRIMITIVE_UNARY_OPERATION_NAMES =
            Predicates.or(UNARY_OPERATION_FOR_PRIMITIVE_NUMBER, pattern("Boolean.not"));
    @NotNull
    private static final DescriptorPredicate NO_PARAMETERS = new DescriptorPredicate() {
        @Override
        public boolean apply(@NotNull FunctionDescriptor descriptor) {
            return !JsDescriptorUtils.hasParameters(descriptor);
        }
    };
    @NotNull
    private static final Predicate<FunctionDescriptor> PATTERN = Predicates.and(PRIMITIVE_UNARY_OPERATION_NAMES, NO_PARAMETERS);

    @Nullable
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        if (!PATTERN.apply(descriptor)) {
            return null;
        }

        Name name = descriptor.getName();
        JetToken jetToken = OperatorConventions.UNARY_OPERATION_NAMES.inverse().get(name);
        final JsUnaryOperator jsOperator = OperatorTable.getUnaryOperator(jetToken);
        return new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(@Nullable JsExpression receiver,
                    @NotNull List<JsExpression> arguments,
                    @NotNull TranslationContext context) {
                assert receiver != null;
                assert arguments.size() == 0 : "Unary operator should not have arguments.";
                //NOTE: cannot use this for increment/decrement
                return new JsPrefixOperation(jsOperator, receiver);
            }
        };
    }

}
