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

import org.jetbrains.kotlin.j2k.ClassKind
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.append

abstract class Member(var annotations: Annotations, val modifiers: Modifiers) : Element()

class ClassBody (
        val primaryConstructor: PrimaryConstructor?,
        val primaryConstructorSignature: PrimaryConstructorSignature?,
        val baseClassParams: List<DeferredElement<Expression>>?,
        val members: List<Member>,
        val companionObjectMembers: List<Member>,
        val lBrace: LBrace,
        val rBrace: RBrace,
        val classKind: ClassKind
) {
    fun appendTo(builder: CodeBuilder) {
        val membersFiltered = members.filter { !it.isEmpty }
        if (classKind != ClassKind.ANONYMOUS_OBJECT && membersFiltered.isEmpty() && companionObjectMembers.isEmpty()) return

        builder append " " append lBrace append "\n"

        if (!classKind.isEnum()) {
            builder.append(membersFiltered, "\n")
        }
        else {
            val (constants, otherMembers) = membersFiltered.partition { it is EnumConstant }

            builder.append(constants, ", ")

            if (otherMembers.isNotEmpty() || companionObjectMembers.isNotEmpty()) {
                builder.append(";\n")
            }

            builder.append(otherMembers, "\n")
        }

        appendCompanionObject(builder, membersFiltered.isNotEmpty())

        builder append "\n" append rBrace
    }

    private fun appendCompanionObject(builder: CodeBuilder, blankLineBefore: Boolean) {
        if (companionObjectMembers.isEmpty()) return
        if (blankLineBefore) builder.append("\n\n")
        builder.append(companionObjectMembers, "\n", "companion object {\n", "\n}")
    }
}
