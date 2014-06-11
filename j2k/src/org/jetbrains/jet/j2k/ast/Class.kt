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

import org.jetbrains.jet.j2k.Converter
import java.util.ArrayList

open class Class(
        val converter: Converter,
        val name: Identifier,
        comments: MemberComments,
        modifiers: Set<Modifier>,
        val typeParameterList: TypeParameterList,
        val extendsTypes: List<Type>,
        val baseClassParams: List<Expression>,
        val implementsTypes: List<Type>,
        val body: ClassBody
) : Member(comments, modifiers) {

    override fun toKotlin(): String =
            commentsToKotlin() +
            modifiersToKotlin() +
            keyword + " " + name.toKotlin() +
            typeParameterList.toKotlin() +
            primaryConstructorSignatureToKotlin() +
            implementTypesToKotlin() +
            typeParameterList.whereToKotlin().withPrefix(" ") +
            body.toKotlin(this)

    protected open val keyword: String
        get() = "class"

    protected open fun primaryConstructorSignatureToKotlin(): String
            = body.primaryConstructor?.signatureToKotlin() ?: "()"

    private fun baseClassSignatureWithParams(): List<String> {
        if (keyword.equals("class") && extendsTypes.size() == 1) {
            val baseParams = baseClassParams.toKotlin(", ")
            return arrayListOf(extendsTypes[0].toKotlin() + "(" + baseParams + ")")
        }
        return extendsTypes.map { it.toKotlin() }
    }

    protected fun implementTypesToKotlin(): String {
        val allTypes = ArrayList<String>()
        allTypes.addAll(baseClassSignatureWithParams())
        allTypes.addAll(implementsTypes.map { it.toKotlin() })
        return if (allTypes.size() == 0)
            ""
        else
            " : " + allTypes.makeString(", ")
    }

    protected open fun modifiersToKotlin(): String {
        val modifierList = ArrayList<Modifier>()

        modifiers.accessModifier()?.let { modifierList.add(it) }

        if (modifiers.contains(Modifier.ABSTRACT)) {
            modifierList.add(Modifier.ABSTRACT)
        }
        else if (modifiers.contains(Modifier.OPEN)) {
            modifierList.add(Modifier.OPEN)
        }

        if (modifiers.contains(Modifier.INNER)) {
            modifierList.add(Modifier.INNER)
        }

        return modifierList.toKotlin()
    }
}
