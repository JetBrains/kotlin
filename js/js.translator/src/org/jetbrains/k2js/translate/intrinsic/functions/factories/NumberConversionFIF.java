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

import closurecompiler.internal.com.google.common.collect.Sets;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.CallStandardMethodIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NameChecker;

import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.*;
import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;

/**
 * @author Pavel Talanov
 */
public final class NumberConversionFIF {
    @NotNull
    private static final NameChecker SUPPORTED_CONVERSIONS;

    static {
        Set<Name> supportedConversions = Sets.newHashSet(NUMBER_CONVERSIONS);
        //TODO: support longs and chars
        supportedConversions.remove(CHAR);
        supportedConversions.remove(LONG);
        SUPPORTED_CONVERSIONS = new NameChecker(supportedConversions);
    }

    @NotNull
    private static final NameChecker FLOATING_POINT_CONVERSIONS = new NameChecker(OperatorConventions.FLOAT, OperatorConventions.DOUBLE);

    @NotNull
    private static final NameChecker INTEGER_CONVERSIONS = new NameChecker(OperatorConventions.INT, OperatorConventions.SHORT,
                                                                           OperatorConventions.BYTE);

    @NotNull
    private static final FunctionIntrinsic RETURN_RECEIVER = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.isEmpty();
            return receiver;
        }
    };

    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = FIFBuilder.start()
            .add(pattern("Int", SUPPORTED_CONVERSIONS), RETURN_RECEIVER)
            .add(pattern("Double|Number", INTEGER_CONVERSIONS), new CallStandardMethodIntrinsic("Math.floor", true, 0))
            .add(pattern("Double|Number", FLOATING_POINT_CONVERSIONS), RETURN_RECEIVER)
            .build();

    private NumberConversionFIF() {
    }
}
