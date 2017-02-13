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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.patterns.DescriptorPredicate;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed;
import org.jetbrains.kotlin.name.Name;

import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.decapitalize;
import static org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.assignment;

public final class ArrayFIF extends CompositeFIF {
    private static final NamePredicate NUMBER_ARRAY;
    private static final NamePredicate CHAR_ARRAY;
    private static final NamePredicate BOOLEAN_ARRAY;
    private static final NamePredicate LONG_ARRAY;
    private static final NamePredicate ARRAYS;
    private static final DescriptorPredicate ARRAY_FACTORY_METHODS;

    static {
        List<Name> arrayTypeNames = Lists.newArrayList();
        List<Name> arrayFactoryMethodNames = Lists.newArrayList(Name.identifier("arrayOf"));
        for (PrimitiveType type : PrimitiveType.values()) {
            Name arrayTypeName = type.getArrayTypeName();
            if (type != PrimitiveType.CHAR && type != PrimitiveType.BOOLEAN && type != PrimitiveType.LONG) {
                arrayTypeNames.add(arrayTypeName);
            }
            arrayFactoryMethodNames.add(Name.identifier(decapitalize(arrayTypeName.asString() + "Of")));
        }

        Name arrayName = KotlinBuiltIns.FQ_NAMES.array.shortName();
        Name booleanArrayName = PrimitiveType.BOOLEAN.getArrayTypeName();
        Name charArrayName = PrimitiveType.CHAR.getArrayTypeName();
        Name longArrayName = PrimitiveType.LONG.getArrayTypeName();

        NUMBER_ARRAY = new NamePredicate(arrayTypeNames);
        CHAR_ARRAY = new NamePredicate(charArrayName);
        BOOLEAN_ARRAY = new NamePredicate(booleanArrayName);
        LONG_ARRAY = new NamePredicate(longArrayName);

        arrayTypeNames.add(charArrayName);
        arrayTypeNames.add(booleanArrayName);
        arrayTypeNames.add(longArrayName);
        arrayTypeNames.add(arrayName);
        ARRAYS = new NamePredicate(arrayTypeNames);
        ARRAY_FACTORY_METHODS = pattern(Namer.KOTLIN_LOWER_NAME, new NamePredicate(arrayFactoryMethodNames));
    }

    private static final FunctionIntrinsic ARRAY_INTRINSIC = new FunctionIntrinsicWithReceiverComputed() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert arguments.size() == 1;
            return arguments.get(0);
        }
    };

    @NotNull
    public static final FunctionIntrinsic GET_INTRINSIC = new FunctionIntrinsicWithReceiverComputed() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.size() == 1 : "Array get expression must have one argument.";
            JsExpression indexExpression = arguments.get(0);
            return new JsArrayAccess(receiver, indexExpression);
        }
    };

    @NotNull
    public static final FunctionIntrinsic SET_INTRINSIC = new FunctionIntrinsicWithReceiverComputed() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.size() == 2 : "Array set expression must have two arguments.";
            JsExpression indexExpression = arguments.get(0);
            JsExpression value = arguments.get(1);
            JsArrayAccess arrayAccess = new JsArrayAccess(receiver, indexExpression);
            return assignment(arrayAccess, value);
        }
    };

    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new ArrayFIF();

    private ArrayFIF() {
        add(pattern(ARRAYS, "get"), GET_INTRINSIC);
        add(pattern(ARRAYS, "set"), SET_INTRINSIC);
        add(pattern(ARRAYS, "<get-size>"), LENGTH_PROPERTY_INTRINSIC);
        add(pattern(ARRAYS, "iterator"), new KotlinFunctionIntrinsic("arrayIterator"));

        add(pattern(NUMBER_ARRAY, "<init>(Int)"), new KotlinFunctionIntrinsic("newArray", JsNumberLiteral.ZERO));
        add(pattern(CHAR_ARRAY, "<init>(Int)"), new KotlinFunctionIntrinsic("newArray", JsNumberLiteral.ZERO));
        add(pattern(BOOLEAN_ARRAY, "<init>(Int)"), new KotlinFunctionIntrinsic("newArray", JsLiteral.FALSE));
        add(pattern(LONG_ARRAY, "<init>(Int)"), new KotlinFunctionIntrinsic("newArray", new JsNameRef(Namer.LONG_ZERO, Namer.kotlinLong())));

        add(pattern(ARRAYS, "<init>(Int,Function1)"), new KotlinFunctionIntrinsic("newArrayF"));

        add(pattern("kotlin", "arrayOfNulls"), new KotlinFunctionIntrinsic("newArray", JsLiteral.NULL));

        add(ARRAY_FACTORY_METHODS, ARRAY_INTRINSIC);
    }
}
