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

package org.jetbrains.k2js.translate.intrinsic.primitive;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;

/**
 * @author Pavel Talanov
 */
public final class PrimitiveRangeToIntrinsic implements FunctionIntrinsic {

    @NotNull
    public static PrimitiveRangeToIntrinsic newInstance() {
        return new PrimitiveRangeToIntrinsic();
    }

    private PrimitiveRangeToIntrinsic() {
    }

    @NotNull
    @Override
    public JsExpression apply(@Nullable JsExpression rangeStart, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 1 : "RangeTo must have one argument.";
        assert rangeStart != null;
        JsExpression rangeEnd = arguments.get(0);
        JsBinaryOperation rangeSize = sum(subtract(rangeEnd, rangeStart),
                                          context.program().getNumberLiteral(1));
        //TODO: provide a way not to hard code this value
        JsNameRef expr = AstUtil.newQualifiedNameRef("Kotlin.NumberRange");
        HasArguments numberRangeConstructorInvocation = context.isEcma5() ? AstUtil.newInvocation(expr) : new JsNew(expr);
        //TODO: add tests and correct expression for reversed ranges.
        JsBooleanLiteral isRangeReversed = context.program().getFalseLiteral();
        setArguments(numberRangeConstructorInvocation, rangeStart, rangeSize, isRangeReversed);
        return (JsExpression) numberRangeConstructorInvocation;
    }
}
