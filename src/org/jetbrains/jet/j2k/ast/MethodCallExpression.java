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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.ast.types.Type;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class MethodCallExpression extends Expression {
    private final Expression myMethodCall;
    private final List<Expression> myArguments;
    private final List<String> myConversions;
    private final boolean myIsResultNullable;
    private final List<Type> myTypeParameters;

    public MethodCallExpression(Expression methodCall, List<Expression> arguments, List<Type> typeParameters) {
        this(methodCall, arguments, AstUtil.createListWithEmptyString(arguments), typeParameters, false);
    }

    public MethodCallExpression(Expression methodCall, List<Expression> arguments, List<String> conversions, List<Type> typeParameters, boolean nullable) {
        myMethodCall = methodCall;
        myArguments = arguments;
        myConversions = conversions;
        myIsResultNullable = nullable;
        myTypeParameters = typeParameters;
    }

    @Override
    public boolean isNullable() {
        return myMethodCall.isNullable() || myIsResultNullable;
    }

    @NotNull
    @Override
    public String toKotlin() {
        String typeParamsToKotlin = myTypeParameters.size() > 0
                                    ? "<" + AstUtil.joinNodes(myTypeParameters, COMMA_WITH_SPACE) + ">"
                                    : EMPTY;
        List<String> applyConversions = AstUtil.applyConversions(AstUtil.nodesToKotlin(myArguments), myConversions);
        return myMethodCall.toKotlin() + typeParamsToKotlin + "(" + AstUtil.join(applyConversions, COMMA_WITH_SPACE) + ")";
    }
}
