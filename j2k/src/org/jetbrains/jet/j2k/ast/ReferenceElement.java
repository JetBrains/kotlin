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

public class ReferenceElement extends Element {
    @NotNull
    private final Identifier myReference;
    @NotNull
    private final List<Type> myTypes;

    public ReferenceElement(@NotNull Identifier reference, @NotNull List<Type> types) {
        myReference = reference;
        myTypes = types;
    }

    @NotNull
    @Override
    public String toKotlin() {
        String typesIfNeeded = myTypes.size() > 0 ? "<" + AstUtil.joinNodes(myTypes, COMMA_WITH_SPACE) + ">" : EMPTY;
        return myReference.toKotlin() + typesIfNeeded;
    }
}
