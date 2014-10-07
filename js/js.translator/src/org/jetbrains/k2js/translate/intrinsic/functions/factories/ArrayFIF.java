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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;

import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.decapitalize;
import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.assignment;

public final class ArrayFIF extends CompositeFIF {

    @NotNull
    private static final NamePredicate NUMBER_ARRAY;

    @NotNull
    private static final NamePredicate CHAR_ARRAY;

    @NotNull
    private static final NamePredicate BOOLEAN_ARRAY;

    @NotNull
    private static final NamePredicate LONG_ARRAY;

    @NotNull
    private static final NamePredicate ARRAY;

    @NotNull
    private static final NamePredicate ARRAYS;

    @NotNull
    private static final DescriptorPredicate ARRAY_FACTORY_METHODS;

    static {
        List<Name> arrayTypeNames = Lists.newArrayList();
        List<Name> arrayFactoryMethodNames = Lists.newArrayList(Name.identifier("array"));
        for (PrimitiveType type : PrimitiveType.values()) {
            Name arrayTypeName = type.getArrayTypeName();
            if (type != PrimitiveType.CHAR && type != PrimitiveType.BOOLEAN && type != PrimitiveType.LONG) {
                arrayTypeNames.add(arrayTypeName);
            }
            arrayFactoryMethodNames.add(Name.identifier(decapitalize(arrayTypeName.asString())));
        }

        Name arrayName = Name.identifier("Array");
        Name booleanArrayName = PrimitiveType.BOOLEAN.getArrayTypeName();
        Name charArrayName = PrimitiveType.CHAR.getArrayTypeName();
        Name longArrayName = PrimitiveType.LONG.getArrayTypeName();

        NUMBER_ARRAY = new NamePredicate(arrayTypeNames);
        CHAR_ARRAY = new NamePredicate(charArrayName);
        BOOLEAN_ARRAY = new NamePredicate(booleanArrayName);
        LONG_ARRAY = new NamePredicate(longArrayName);
        ARRAY = new NamePredicate(arrayName);

        arrayTypeNames.add(charArrayName);
        arrayTypeNames.add(booleanArrayName);
        arrayTypeNames.add(longArrayName);
        arrayTypeNames.add(arrayName);
        ARRAYS = new NamePredicate(arrayTypeNames);
        ARRAY_FACTORY_METHODS = pattern(Namer.KOTLIN_LOWER_NAME, new NamePredicate(arrayFactoryMethodNames));
    }

    private static final FunctionIntrinsic ARRAY_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert arguments.size() == 1;
            return arguments.get(0);
        }
    };

    @NotNull
    public static final FunctionIntrinsic GET_INTRINSIC = new FunctionIntrinsic() {
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
    public static final FunctionIntrinsic SET_INTRINSIC = new FunctionIntrinsic() {
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
            return assignment(arrayAccess, value);
        }
    };

    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new ArrayFIF();

    private ArrayFIF() {
        add(pattern(ARRAYS, "get"), GET_INTRINSIC);
        add(pattern(ARRAYS, "set"), SET_INTRINSIC);
        add(pattern(ARRAYS, "<get-size>"), LENGTH_PROPERTY_INTRINSIC);
        add(pattern(ARRAYS, "<get-indices>"), new KotlinFunctionIntrinsic("arrayIndices"));
        add(pattern(ARRAYS, "iterator"), new KotlinFunctionIntrinsic("arrayIterator"));
        add(pattern(ARRAY, "<init>"), new KotlinFunctionIntrinsic("arrayFromFun"));
        add(pattern(NUMBER_ARRAY, "<init>"),new KotlinFunctionIntrinsic("numberArrayOfSize"));
        add(pattern(CHAR_ARRAY, "<init>"), new KotlinFunctionIntrinsic("charArrayOfSize"));
        add(pattern(BOOLEAN_ARRAY, "<init>"), new KotlinFunctionIntrinsic("booleanArrayOfSize"));
        add(pattern(LONG_ARRAY, "<init>"), new KotlinFunctionIntrinsic("longArrayOfSize"));
        add(ARRAY_FACTORY_METHODS, ARRAY_INTRINSIC);
    }
}
