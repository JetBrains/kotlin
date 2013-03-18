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

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsArrayAccess;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.BuiltInPropertyIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.CallStandardMethodIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;

public final class ArrayFIF extends CompositeFIF {

    @NotNull
    private static final NamePredicate NUMBER_ARRAY;

    @NotNull
    private static final NamePredicate CHAR_ARRAY;

    @NotNull
    private static final NamePredicate BOOLEAN_ARRAY;

    @NotNull
    private static final NamePredicate ARRAY;

    @NotNull
    private static final NamePredicate ARRAYS;
    static {
        List<Name> arrayTypeNames = Lists.newArrayList();
        for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
            if (type != PrimitiveType.CHAR) {
                arrayTypeNames.add(type.getArrayTypeName());
            }
        }

        Name arrayName = Name.identifier("Array");
        Name booleanArrayName = PrimitiveType.BOOLEAN.getArrayTypeName();
        Name charArrayName = PrimitiveType.CHAR.getArrayTypeName();

        NUMBER_ARRAY = new NamePredicate(arrayTypeNames);
        CHAR_ARRAY = new NamePredicate(charArrayName);
        BOOLEAN_ARRAY = new NamePredicate(booleanArrayName);
        ARRAY = new NamePredicate(arrayName);

        arrayTypeNames.add(charArrayName);
        arrayTypeNames.add(booleanArrayName);
        arrayTypeNames.add(arrayName);
        ARRAYS = new NamePredicate(arrayTypeNames);
    }

    @NotNull
    private static final FunctionIntrinsic GET_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.size() == 1 : "Array get expression must have one argument.";
            JsExpression indexExpression = arguments.get(0);
            return new JsArrayAccess(receiver, indexExpression);
        }
    };

    @NotNull
    private static final FunctionIntrinsic SET_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.size() == 2 : "Array set expression must have two arguments.";
            JsExpression indexExpression = arguments.get(0);
            JsExpression value = arguments.get(1);
            JsArrayAccess arrayAccess = new JsArrayAccess(receiver, indexExpression);
            return JsAstUtils.assignment(arrayAccess, value);
        }
    };

    @NotNull
    public static final BuiltInPropertyIntrinsic ARRAY_LENGTH_INTRINSIC = new BuiltInPropertyIntrinsic("length");

    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new ArrayFIF();

    private ArrayFIF() {
        add(pattern(ARRAYS, "get"), GET_INTRINSIC);
        add(pattern(ARRAYS, "set"), SET_INTRINSIC);
        add(pattern(ARRAYS, "<get-size>"), ARRAY_LENGTH_INTRINSIC);
        add(pattern(ARRAYS, "<get-indices>"), new CallStandardMethodIntrinsic(new JsNameRef("arrayIndices", "Kotlin"), true, 0));
        add(pattern(ARRAYS, "iterator"), new CallStandardMethodIntrinsic(new JsNameRef("arrayIterator", "Kotlin"), true, 0));
        add(pattern(ARRAY, "<init>"), new CallStandardMethodIntrinsic(new JsNameRef("arrayFromFun", "Kotlin"), false, 2));
        add(pattern(NUMBER_ARRAY, "<init>"), new CallStandardMethodIntrinsic(new JsNameRef("numberArrayOfSize", "Kotlin"), false, 1));
        add(pattern(CHAR_ARRAY, "<init>"), new CallStandardMethodIntrinsic(new JsNameRef("charArrayOfSize", "Kotlin"), false, 1));
        add(pattern(BOOLEAN_ARRAY, "<init>"), new CallStandardMethodIntrinsic(new JsNameRef("booleanArrayOfSize", "Kotlin"), false, 1));
    }
}
