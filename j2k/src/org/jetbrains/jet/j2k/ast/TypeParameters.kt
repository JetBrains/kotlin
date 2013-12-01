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
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.jet.j2k.Converter
import com.intellij.psi.PsiTypeParameterList
import java.util.ArrayList

public class TypeParameter(val name: Identifier, val extendsTypes: List<Type>) : Element {
    public fun hasWhere(): Boolean = extendsTypes.size() > 1
    public fun getWhereToKotlin(): String {
        if (hasWhere()) {
            return name.toKotlin() + " : " + extendsTypes.get(1).toKotlin()
        }

        return ""
    }

    public override fun toKotlin(): String {
        if (extendsTypes.size() > 0) {
            return name.toKotlin() + " : " + extendsTypes [0].toKotlin()
        }

        return name.toKotlin()
    }
}

public class TypeParameterList(val parameters: List<TypeParameter>) : Element {
    override fun toKotlin(): String = if (!parameters.isEmpty())
        parameters.map {
            it.toKotlin()
        }.makeString(", ", "<", ">")
    else ""

    public fun whereToKotlin(): String {
        if (hasWhere()) {
            val wheres = parameters.map { it.getWhereToKotlin() }
            return "where " + wheres.makeString(", ")
        }
        return ""
    }

    override fun isEmpty(): Boolean = parameters.isEmpty()

    private fun hasWhere(): Boolean = parameters.any { it.hasWhere() }

    class object {
        public val Empty: TypeParameterList = TypeParameterList(ArrayList())
    }
}

public fun Converter.typeParameterToTypeParameter(psiTypeParameter: PsiTypeParameter): TypeParameter {
    return elementToElement(psiTypeParameter) as TypeParameter
}

public fun Converter.typeParameterListToTypeParameterList(psiTypeParameterlist: PsiTypeParameterList?): TypeParameterList {
    return if (psiTypeParameterlist == null) TypeParameterList.Empty
    else TypeParameterList(psiTypeParameterlist.getTypeParameters()!!.toList().map { typeParameterToTypeParameter(it) })
}