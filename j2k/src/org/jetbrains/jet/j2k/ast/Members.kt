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

class MemberComments(elements: List<Element>) : WhiteSpaceSeparatedElementList(elements, WhiteSpace.NoSpace) {
    class object {
        val Empty = MemberComments(ArrayList())
    }
}

abstract class Member(val comments: MemberComments, val annotations: Annotations, val modifiers: Set<Modifier>) : Element() {
    fun commentsToKotlin(commentConverter: CommentConverter): String = comments.toKotlin(commentConverter)
}

//member itself and all the elements before it in the code (comments, whitespaces)
class MemberWithComments(val member: Member, val elements: List<Element>)

class MemberList(elements: List<Element>) : WhiteSpaceSeparatedElementList(elements, WhiteSpace.NewLine) {
    val members: List<Member>
        get() = elements.filter { it is Member }.map { it as Member }
}

class ClassBody (
        val primaryConstructor: PrimaryConstructor?,
        val secondaryConstructors: MemberList,
        val normalMembers: MemberList,
        val classObjectMembers: MemberList) {

    fun toKotlin(containingClass: Class?, commentConverter: CommentConverter): String {
        val innerBody = normalMembers.toKotlin(commentConverter) +
                primaryConstructorBodyToKotlin(commentConverter) +
                classObjectToKotlin(containingClass, commentConverter)
        return if (innerBody.trim().isNotEmpty()) " {" + innerBody + "}" else ""
    }

    private fun primaryConstructorBodyToKotlin(commentConverter: CommentConverter): String {
        val constructor = primaryConstructor
        if (constructor != null && !(constructor.block?.isEmpty ?: true)) {
            return "\n" + constructor.bodyToKotlin(commentConverter) + "\n"
        }
        return ""
    }

    private fun classObjectToKotlin(containingClass: Class?, commentConverter: CommentConverter): String {
        val secondaryConstructorsAsStaticInitFunctions = secondaryConstructorsAsStaticInitFunctions(containingClass)
        if (secondaryConstructorsAsStaticInitFunctions.isEmpty() && classObjectMembers.isEmpty()) return ""
        return "\nclass object {${secondaryConstructorsAsStaticInitFunctions.toKotlin(commentConverter)}${classObjectMembers.toKotlin(commentConverter)}}"
    }

    private fun secondaryConstructorsAsStaticInitFunctions(containingClass: Class?): MemberList {
        return MemberList(secondaryConstructors.elements.map { if (it is SecondaryConstructor && containingClass != null) it.toInitFunction(containingClass) else it })
    }
}
