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

public class CallChainExpression extends Expression {
    private final Expression myExpression;
    private final Expression myIdentifier;

    public Expression getIdentifier() {
        return myIdentifier;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.CALL_CHAIN;
    }

    public CallChainExpression(Expression expression, Expression identifier) {
        myExpression = expression;
        myIdentifier = identifier;
    }

    @Override
    public boolean isNullable() {
        return myExpression.isNullable() || myIdentifier.isNullable();
    }

    @NotNull
    @Override
    public String toKotlin() {
        if (!myExpression.isEmpty()) {
            if (myExpression.isNullable()) {
                return myExpression.toKotlin() + QUESTDOT + myIdentifier.toKotlin();
            }
            else {
                return myExpression.toKotlin() + DOT + myIdentifier.toKotlin();
            }
        }
        return myIdentifier.toKotlin();
    }
}
