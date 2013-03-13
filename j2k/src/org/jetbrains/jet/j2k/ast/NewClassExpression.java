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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

public class NewClassExpression extends Expression {
    private final Element myName;
    private final List<Expression> myArguments;
    private Expression myQualifier;
    private List<String> myConversions;
    @Nullable
    private AnonymousClass myAnonymousClass = null;

    private NewClassExpression(Element name, List<Expression> arguments) {
        myName = name;
        myQualifier = EMPTY_EXPRESSION;
        myArguments = arguments;
        myConversions = AstUtil.createListWithEmptyString(arguments);
    }

    public NewClassExpression(@NotNull Expression qualifier, @NotNull Element name, @NotNull List<Expression> arguments,
                              @NotNull List<String> conversions, @Nullable AnonymousClass anonymousClass) {
        this(name, arguments);
        myQualifier = qualifier;
        myConversions = conversions;
        myAnonymousClass = anonymousClass;
    }

    @NotNull
    @Override
    public String toKotlin() {
        String callOperator = myQualifier.isNullable() ? QUESTDOT : DOT;
        String qualifier = myQualifier.isEmpty() ? EMPTY : myQualifier.toKotlin() + callOperator;
        List<String> applyConversions = AstUtil.applyConversions(AstUtil.nodesToKotlin(myArguments), myConversions);
        String appliedArguments = AstUtil.join(applyConversions, COMMA_WITH_SPACE);
        return myAnonymousClass != null ?
               "object" + SPACE + ":" + SPACE + qualifier + myName.toKotlin() + "(" + appliedArguments + ")" + myAnonymousClass.toKotlin()
                                        :
               qualifier + myName.toKotlin() + "(" + appliedArguments + ")";
    }
}
