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

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.CompareToIntrinsic;
import org.jetbrains.k2js.translate.operation.OperatorTable;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class PrimitiveCompareToIntrinsic extends CompareToIntrinsic {

    @NotNull
    public static PrimitiveCompareToIntrinsic newInstance() {
        return new PrimitiveCompareToIntrinsic();
    }

    private PrimitiveCompareToIntrinsic() {
    }

    @NotNull
    public JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 1 : "Equals operation should have one argument";
        JsBinaryOperator operator = OperatorTable.getBinaryOperator(getComparisonToken());
        JsExpression argument = arguments.get(0);
        return new JsBinaryOperation(operator, receiver, argument);
    }

}
