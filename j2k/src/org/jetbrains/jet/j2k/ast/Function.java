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
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Function extends Member {
    private final Identifier myName;
    private final Type myType;
    private final List<Element> myTypeParameters;
    final Element myParams;

    // TODO: maybe remove it?
    public void setBlock(Block block) {
        myBlock = block;
    }

    Block myBlock;

    public Function(Identifier name, Set<String> modifiers, Type type, List<Element> typeParameters, Element params, Block block) {
        myName = name;
        myModifiers = modifiers;
        myType = type;
        myTypeParameters = typeParameters;
        myParams = params;
        myBlock = block;
    }

    public List<Element> getTypeParameters() {
        return myTypeParameters;
    }

    public Element getParams() {
        return myParams;
    }

    public Block getBlock() {
        return myBlock;
    }

    @NotNull
    private String typeParametersToKotlin() {
        return myTypeParameters.size() > 0 ? "<" + AstUtil.joinNodes(myTypeParameters, COMMA_WITH_SPACE) + ">" : EMPTY;
    }

    private boolean hasWhere() {
        for (Element t : myTypeParameters)
            if (t instanceof TypeParameter && ((TypeParameter) t).hasWhere()) {
                return true;
            }
        return false;
    }

    private String typeParameterWhereToKotlin() {
        if (hasWhere()) {
            List<String> wheres = new LinkedList<String>();
            for (Element t : myTypeParameters)
                if (t instanceof TypeParameter) {
                    wheres.add(((TypeParameter) t).getWhereToKotlin());
                }
            return SPACE + "where" + SPACE + AstUtil.join(wheres, COMMA_WITH_SPACE) + SPACE;
        }
        return EMPTY;
    }

    String modifiersToKotlin() {
        List<String> modifierList = new LinkedList<String>();

        String accessModifier = accessModifier();
        if (!accessModifier.isEmpty()) {
            modifierList.add(accessModifier);
        }

        if (isAbstract()) {
            modifierList.add(Modifier.ABSTRACT);
        }

        if (myModifiers.contains(Modifier.OVERRIDE)) {
            modifierList.add(Modifier.OVERRIDE);
        }

        if (!myModifiers.contains(Modifier.ABSTRACT) && !myModifiers.contains(Modifier.OVERRIDE) && !myModifiers.contains(Modifier.FINAL)) {
            modifierList.add(Modifier.OPEN);
        }

        if (myModifiers.contains(Modifier.NOT_OPEN)) {
            modifierList.remove(Modifier.OPEN);
        }

        if (modifierList.size() > 0) {
            return AstUtil.join(modifierList, SPACE) + SPACE;
        }

        return EMPTY;
    }

    @NotNull
    @Override
    public String toKotlin() {
        return modifiersToKotlin() + "fun" + SPACE + myName.toKotlin() + typeParametersToKotlin() + "(" + myParams.toKotlin() + ")" + SPACE + COLON +
               SPACE + myType.toKotlin() + SPACE +
               typeParameterWhereToKotlin() +
               myBlock.toKotlin();
    }
}
