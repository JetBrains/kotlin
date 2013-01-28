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

public class Parameter extends Expression {
    private final Identifier myIdentifier;
    private final Type myType;
    private boolean myReadOnly;

    public Parameter(Identifier identifier, Type type) {
        myIdentifier = identifier;
        myType = type;
        myReadOnly = true;
    }

    public Parameter(IdentifierImpl identifier, Type type, boolean readOnly) {
        this(identifier, type);
        myReadOnly = readOnly;
    }

    @NotNull
    @Override
    public String toKotlin() {
        String vararg = myType.getKind() == Kind.VARARG ? "vararg" + SPACE : EMPTY;
        String var = myReadOnly ? EMPTY : "var" + SPACE;
        return vararg + var + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();
    }
}
