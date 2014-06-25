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

import org.jetbrains.jet.j2k.*

abstract class Member(val annotations: Annotations, val modifiers: Modifiers) : Element()

class ClassBody (
        val primaryConstructorSignature: PrimaryConstructorSignature?,
        val members: List<Member>,
        val secondaryConstructors: List<SecondaryConstructor>,
        val classObjectMembers: List<Member>,
        val lBrace: LBrace,
        val rBrace: RBrace) {

    fun append(builder: CodeBuilder, containingClass: Class?) {
        if (members.isEmpty() && classObjectMembers.isEmpty() && secondaryConstructors.isEmpty()) return

        builder append " " append lBrace append "\n"

        builder.append(members, "\n")

        appendClassObject(builder, containingClass, members.isNotEmpty())

        builder append "\n" append rBrace
    }

    private fun appendClassObject(builder: CodeBuilder, containingClass: Class?, blankLineBefore: Boolean) {
        if (secondaryConstructors.isEmpty() && classObjectMembers.isEmpty()) return
        if (blankLineBefore) builder.append("\n\n")
        val factoryFunctions = secondaryConstructors.map { it.toFactoryFunction(containingClass) }
        builder.append(factoryFunctions + classObjectMembers, "\n", "class object {\n", "\n}")
    }
}
