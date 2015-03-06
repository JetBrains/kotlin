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

import org.jetbrains.kotlin.j2k.*

abstract class Member(val annotations: Annotations, val modifiers: Modifiers) : Element()

class ClassBody (
        val primaryConstructorSignature: PrimaryConstructorSignature?,
        val baseClassParams: List<DeferredElement<Expression>>,
        val members: List<Member>,
        val defaultObjectMembers: List<Member>,
        val factoryFunctions: List<FactoryFunction>,
        val lBrace: LBrace,
        val rBrace: RBrace) {

    fun append(builder: CodeBuilder) {
        val membersFiltered = members.filter { !it.isEmpty }
        if (membersFiltered.isEmpty() && defaultObjectMembers.isEmpty()) return

        builder append " " append lBrace append "\n"

        builder.append(membersFiltered, "\n")

        appendDefaultObject(builder, membersFiltered.isNotEmpty())

        builder append "\n" append rBrace
    }

    private fun appendDefaultObject(builder: CodeBuilder, blankLineBefore: Boolean) {
        if (defaultObjectMembers.isEmpty()) return
        if (blankLineBefore) builder.append("\n\n")
        builder.append(defaultObjectMembers, "\n", "default object {\n", "\n}")
    }
}
