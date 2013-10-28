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

package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type
import java.util.LinkedList
import org.jetbrains.jet.j2k.Converter
import java.util.ArrayList

public open class Field(val identifier : Identifier,
                        val docComments: List<Node>,
                        modifiers : Set<Modifier>,
                        val `type` : Type,
                        val initializer : Element,
                        val writingAccesses : Int) : Member(modifiers) {

    open fun modifiersToKotlin() : String {
        val modifierList = ArrayList<Modifier>()
        if (isAbstract()) {
            modifierList.add(Modifier.ABSTRACT)
        }

        val modifier = accessModifier()
        if (modifier != null) {
          modifierList.add(modifier)
        }

        return modifierList.toKotlin() + (if (isVal()) "val " else "var ")
    }

    public open fun isVal() : Boolean = modifiers.contains(Modifier.FINAL)
    public override fun isStatic() : Boolean = modifiers.contains(Modifier.STATIC)

    public override fun toKotlin() : String {
        val declaration : String = docComments.toKotlin("\n", "", "\n") +
                                   modifiersToKotlin() + identifier.toKotlin() + " : " + `type`.toKotlin()
        if (initializer.isEmpty()) {
            return declaration + ((if (isVal() && !isStatic() && writingAccesses != 0)
                ""
            else
                " = " + Converter.getDefaultInitializer(this)))
        }

        return declaration + " = " + initializer.toKotlin()
    }
}
