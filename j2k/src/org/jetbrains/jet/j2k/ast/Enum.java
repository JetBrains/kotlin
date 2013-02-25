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
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;
import java.util.Set;

public class Enum extends Class {
    public Enum(Converter converter, Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes,
                List<Expression> baseClassParams, List<Type> implementsTypes, List<Member> members) {
        super(converter, name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, getMembers(members, converter));
    }

    String primaryConstructorSignatureToKotlin() {
        String s = super.primaryConstructorSignatureToKotlin();
        return s.equals("()") ? EMPTY : s;
    }

    @Override
    boolean needOpenModifier() {
        return false;
    }

    @NotNull
    @Override
    public String toKotlin() {
        String primaryConstructorBody = primaryConstructorBodyToKotlin();
        return modifiersToKotlin() + "enum class" + SPACE + myName.toKotlin() + primaryConstructorSignatureToKotlin() +
               typeParametersToKotlin() + implementTypesToKotlin() + SPACE + "{" + N +
               AstUtil.joinNodes(membersExceptConstructors(), N) + N +
               (primaryConstructorBody.isEmpty() ? EMPTY : primaryConstructorBody + N) +
               "}";
    }
}
