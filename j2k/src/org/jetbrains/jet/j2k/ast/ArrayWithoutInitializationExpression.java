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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

public class ArrayWithoutInitializationExpression extends Expression {
    private final Type myType;
    private final List<Expression> myExpressions;

    public ArrayWithoutInitializationExpression(Type type, List<Expression> expressions) {
        myType = type;
        myExpressions = expressions;
    }

    @NotNull
    @Override
    public String toKotlin() {
        if (myType.getKind() == Kind.ARRAY_TYPE) {
            return constructInnerType((ArrayType) myType, myExpressions);
        }
        return getConstructorName(myType);
    }

    @NotNull
    private static String constructInnerType(@NotNull ArrayType hostType, @NotNull List<Expression> expressions) {
        if (expressions.size() == 1) {
            return oneDim(hostType, expressions.get(0));
        }
        Type innerType = hostType.getInnerType();
        if (expressions.size() > 1 && innerType.getKind() == Kind.ARRAY_TYPE) {
            return oneDim(hostType, expressions.get(0), "{" + constructInnerType((ArrayType) innerType, expressions.subList(1, expressions.size())) + "}");
        }
        return getConstructorName(hostType);
    }

    @NotNull
    private static String oneDim(@NotNull Type type, @NotNull Expression size) {
        return oneDim(type, size, EMPTY);
    }

    @NotNull
    private static String oneDim(@NotNull Type type, @NotNull Expression size, @NotNull String init) {
        String commaWithInit = init.isEmpty() ? EMPTY : COMMA_WITH_SPACE + init;
        return getConstructorName(type) + "(" + size.toKotlin() + commaWithInit + ")";
    }

    @NotNull
    private static String getConstructorName(@NotNull Type type) {
        return AstUtil.replaceLastQuest(type.toKotlin());
    }
}
