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

import closurecompiler.internal.com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NameChecker;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.Pattern;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public enum TupleGetterFIF implements FunctionIntrinsicFactory {
    INSTANCE;

    @NotNull
    private static final NameChecker TUPLES;

    static {
        List<String> tupleNames = Lists.newArrayList();
        for (int tupleSize = 0; tupleSize <= JetStandardClasses.MAX_TUPLE_ORDER; ++tupleSize) {
            tupleNames.add("Tuple" + tupleSize);
        }
        TUPLES = new NameChecker(tupleNames);
    }

    @NotNull
    private static final NameChecker TUPLE_UNDERSCORE_ACCESSORS;

    static {
        List<String> accessorNames = Lists.newArrayList();
        for (int tupleSize = 0; tupleSize <= JetStandardClasses.MAX_TUPLE_ORDER; ++tupleSize) {
            accessorNames.add("<get-_" + (tupleSize + 1) + ">");
        }
        TUPLE_UNDERSCORE_ACCESSORS = new NameChecker(accessorNames);
    }

    @NotNull
    @Override
    public Pattern getPattern() {
        return PatternBuilder.pattern(TUPLES, TUPLE_UNDERSCORE_ACCESSORS);
    }

    @NotNull
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        String nameString = descriptor.getName().getName();
        String elementIndexSubString = nameString.substring(6, nameString.length() - 1);
        final int elementIndex = Integer.parseInt(elementIndexSubString) - 1;
        return new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(@Nullable JsExpression receiver,
                    @NotNull List<JsExpression> arguments,
                    @NotNull TranslationContext context) {
                assert arguments.isEmpty() : "Tuple access expression should not have any arguments.";
                return AstUtil.newArrayAccess(receiver, context.program().getNumberLiteral(elementIndex));
            }
        };
    }
}
