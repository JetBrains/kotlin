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

import java.util.List;

/**
 * @author ignatov
 */
public class TypeParameter extends Element {
    private final Identifier myName;
    private final List<Type> myExtendsTypes;

    public TypeParameter(Identifier name, List<Type> extendsTypes) {
        myName = name;
        myExtendsTypes = extendsTypes;
    }

    public boolean hasWhere() {
        return myExtendsTypes.size() > 1;
    }

    @NotNull
    public String getWhereToKotlin() {
        if (hasWhere()) {
            return myName.toKotlin() + SPACE + COLON + SPACE + myExtendsTypes.get(1).toKotlin();
        }
        return EMPTY;
    }

    @NotNull
    @Override
    public String toKotlin() {
        if (myExtendsTypes.size() > 0) {
            return myName.toKotlin() + SPACE + COLON + SPACE + myExtendsTypes.get(0).toKotlin();
        }
        return myName.toKotlin();
    }
}
