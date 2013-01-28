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

public class ForeachStatement extends Statement {
    private final Parameter myVariable;
    private final Expression myExpression;
    private final Statement myStatement;

    public ForeachStatement(Parameter variable, Expression expression, Statement statement) {
        myVariable = variable;
        myExpression = expression;
        myStatement = statement;
    }

    @NotNull
    @Override
    public String toKotlin() {
        return "for" + SPACE + "(" + myVariable.toKotlin() + SPACE + IN + SPACE + myExpression.toKotlin() + ")" + N +
               myStatement.toKotlin();
    }
}
