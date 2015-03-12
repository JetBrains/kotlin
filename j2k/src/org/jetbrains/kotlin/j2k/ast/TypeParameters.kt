/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.ast

import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.j2k.*
import com.intellij.psi.PsiTypeParameterList

class TypeParameter(val name: Identifier, val extendsTypes: List<Type>) : Element() {
    fun hasWhere(): Boolean = extendsTypes.size() > 1

    fun whereToKotlin(builder: CodeBuilder) {
        if (hasWhere()) {
            builder append name append " : " append extendsTypes[1]
        }
    }

    override fun generateCode(builder: CodeBuilder) {
        builder append name
        if (extendsTypes.isNotEmpty()) {
            builder append " : " append extendsTypes[0]
        }
    }
}

class TypeParameterList(val parameters: List<TypeParameter>) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        if (parameters.isNotEmpty()) builder.append(parameters, ", ", "<", ">")
    }

    fun appendWhere(builder: CodeBuilder): CodeBuilder {
        if (hasWhere()) {
            builder.append( parameters.map { { it.whereToKotlin(builder) } }, ", ", " where ", "")
        }
        return builder
    }

    override val isEmpty: Boolean
        get() = parameters.isEmpty()

    private fun hasWhere(): Boolean = parameters.any { it.hasWhere() }

    default object {
        val Empty = TypeParameterList(listOf())
    }
}

private fun Converter.convertTypeParameter(typeParameter: PsiTypeParameter): TypeParameter {
    return TypeParameter(typeParameter.declarationIdentifier(),
                           typeParameter.getExtendsListTypes().map { typeConverter.convertType(it) }).assignPrototype(typeParameter)
}

fun Converter.convertTypeParameterList(typeParameterList: PsiTypeParameterList?): TypeParameterList {
    return if (typeParameterList != null)
        TypeParameterList(typeParameterList.getTypeParameters()!!.toList().map { convertTypeParameter(it) }).assignPrototype(typeParameterList)
    else
        TypeParameterList.Empty
}
