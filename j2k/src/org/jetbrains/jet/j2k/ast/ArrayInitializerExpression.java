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
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;

import java.util.*;

public class ArrayInitializerExpression extends Expression {
    private final Type myType;
    private final List<Expression> myInitializers;

    public ArrayInitializerExpression(Type type, List<Expression> initializers) {
        myType = type;
        myInitializers = initializers;
    }

    @NotNull
    private static String createArrayFunction(@NotNull Type type) {
        String sType = innerTypeStr(type);
        if (PRIMITIVE_TYPES.contains(sType)) {
            return sType + "Array"; // intArray
        }
        return AstUtil.lowerFirstCharacter(type.convertedToNotNull().toKotlin()); // array<Foo?>
    }

    @NotNull
    private static String innerTypeStr(@NotNull Type type) {
        return type.convertedToNotNull().toKotlin().replace("Array", "").toLowerCase();
    }

    @NotNull
    private static String createInitializers(@NotNull Type type, @NotNull List<Expression> initializers) {
        List<String> arguments = new LinkedList<String>();
        for (Expression i : initializers)
            arguments.add(explicitConvertIfNeeded(type, i));
        return AstUtil.join(arguments, COMMA_WITH_SPACE);
    }

    @NotNull
    private static String explicitConvertIfNeeded(@NotNull Type type, @NotNull Expression i) {
        Set<String> doubleOrFloatTypes = new HashSet<String>(
                Arrays.asList("double", "float", "java.lang.double", "java.lang.float")
        );
        String afterReplace = innerTypeStr(type).replace(">", "").replace("<", "").replace("?", "");
        if (doubleOrFloatTypes.contains(afterReplace)) {
            if (i.getKind() == Kind.LITERAL) {
                if (i.toKotlin().contains(".")) {
                    return i.toKotlin();
                }
                return i.toKotlin() + DOT + ZERO;
            }
            return "(" + i.toKotlin() + ")" + getConversion(afterReplace);
        }
        return i.toKotlin();
    }

    @NotNull
    private static String getConversion(@NotNull String afterReplace) {
        if (afterReplace.contains("double")) return DOT + OperatorConventions.DOUBLE + "()";
        if (afterReplace.contains("float")) return DOT + OperatorConventions.FLOAT + "()";
        return "";
    }

    @NotNull
    @Override
    public String toKotlin() {
        return createArrayFunction(myType) + "(" + createInitializers(myType, myInitializers) + ")";
    }
}
