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
import java.util.HashSet
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
        val bodyElements: List<Element>
) : Member(comments, modifiers) {

    override fun toKotlin(): String =
            commentsToKotlin() +
            modifiersToKotlin() +
            keyword + " " + name.toKotlin() +
            typeParameterList.toKotlin() +
            primaryConstructorSignatureToKotlin() +
            implementTypesToKotlin() +
            typeParameterList.whereToKotlin().withPrefix(" ") +
            bodyToKotlin()

    protected open val keyword: String
        get() = "class"

    protected val classMembers: ClassMembers = ClassMembers.fromBodyElements(bodyElements)

    protected open fun primaryConstructorSignatureToKotlin(): String
            = classMembers.primaryConstructor?.signatureToKotlin() ?: "()"

    protected fun primaryConstructorBodyToKotlin(): String {
        val constructor = classMembers.primaryConstructor
        if (constructor != null && !(constructor.block?.isEmpty ?: true)) {
            return "\n" + constructor.bodyToKotlin() + "\n"
        }
        return ""
    }

    private fun secondaryConstructorsAsStaticInitFunctions(): MemberList {
        return MemberList(classMembers.secondaryConstructors.elements.map { if (it is SecondaryConstructor) it.toInitFunction(this) else it })
    }

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

    protected fun modifiersToKotlin(): String {
        val modifierList = ArrayList<Modifier>()
        val modifier = modifiers.accessModifier()
        if (modifier != null) {
            modifierList.add(modifier)
        }
        if (isAbstract()) {
            modifierList.add(Modifier.ABSTRACT)
        }
        else if (needsOpenModifier()) {
            modifierList.add(Modifier.OPEN)
        }
        return modifierList.toKotlin()
    }

    protected open fun isDefinitelyFinal(): Boolean
            = modifiers.contains(Modifier.FINAL)

    protected open fun needsOpenModifier(): Boolean
            = !isDefinitelyFinal() && converter.settings.openByDefault

    fun bodyToKotlin(): String {
        val innerBody = classMembers.nonStaticMembers.toKotlin() + primaryConstructorBodyToKotlin() + classObjectToKotlin()
        return if (innerBody.trim().isNotEmpty()) " {" + innerBody + "}" else ""
    }

    private fun classObjectToKotlin(): String {
        val secondaryConstructorsAsStaticInitFunctions = secondaryConstructorsAsStaticInitFunctions()
        val staticMembers = classMembers.staticMembers
        if (secondaryConstructorsAsStaticInitFunctions.isEmpty() && staticMembers.isEmpty()) {
            return ""
        }
        return "\nclass object {${secondaryConstructorsAsStaticInitFunctions.toKotlin()}${staticMembers.toKotlin()}}"
    }
}
