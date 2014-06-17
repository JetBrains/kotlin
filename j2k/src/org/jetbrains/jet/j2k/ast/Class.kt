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

import java.util.ArrayList
import org.jetbrains.jet.j2k.CommentConverter

open class Class(
        val name: Identifier,
        comments: MemberComments,
        annotations: Annotations,
        modifiers: Set<Modifier>,
        val typeParameterList: TypeParameterList,
        val extendsTypes: List<Type>,
        val baseClassParams: List<Expression>,
        val implementsTypes: List<Type>,
        val body: ClassBody
) : Member(comments, annotations, modifiers) {

    override fun toKotlinImpl(commentConverter: CommentConverter): String =
            commentsToKotlin(commentConverter) +
            annotations.toKotlin(commentConverter) +
            modifiersToKotlin() +
            keyword + " " + name.toKotlin(commentConverter) +
            typeParameterList.toKotlin(commentConverter) +
            primaryConstructorSignatureToKotlin(commentConverter) +
            implementTypesToKotlin(commentConverter) +
            typeParameterList.whereToKotlin(commentConverter).withPrefix(" ") +
            body.toKotlin(this, commentConverter)

    protected open val keyword: String
        get() = "class"

    protected open fun primaryConstructorSignatureToKotlin(commentConverter: CommentConverter): String
            = body.primaryConstructor?.signatureToKotlin(commentConverter) ?: "()"

    private fun baseClassSignatureWithParams(commentConverter: CommentConverter): List<String> {
        if (keyword.equals("class") && extendsTypes.size() == 1) {
            val baseParams = baseClassParams.toKotlin(commentConverter, ", ")
            return arrayListOf(extendsTypes[0].toKotlin(commentConverter) + "(" + baseParams + ")")
        }
        return extendsTypes.map { it.toKotlin(commentConverter) }
    }

    protected fun implementTypesToKotlin(commentConverter: CommentConverter): String {
        val allTypes = ArrayList<String>()
        allTypes.addAll(baseClassSignatureWithParams(commentConverter))
        allTypes.addAll(implementsTypes.map { it.toKotlin(commentConverter) })
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
