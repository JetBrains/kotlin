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

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.BuiltInFunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.BuiltInPropertyIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;

public final class StringOperationFIF extends CompositeFIF {
    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new StringOperationFIF();

    private StringOperationFIF() {
        add(pattern("jet", "String", "get"), new BuiltInFunctionIntrinsic("charAt"));

        BuiltInPropertyIntrinsic lengthIntrinsic = new BuiltInPropertyIntrinsic("length");
        add(pattern("jet", "String", "<get-length>"), lengthIntrinsic);
        add(pattern("js", "<get-size>").receiverExists(true), lengthIntrinsic);
        add(pattern("js", "length").receiverExists(true), lengthIntrinsic);
        add(pattern("jet", "CharSequence", "<get-length>"), lengthIntrinsic);
        add(pattern("js", "isEmpty").receiverExists(true), new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(
                    @Nullable JsExpression receiver, @NotNull List<JsExpression> arguments, @NotNull TranslationContext context
            ) {
                assert receiver != null;
                return JsAstUtils.equality(new JsNameRef("length", receiver), context.program().getNumberLiteral(0));
            }
        });
    }
}
