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

//TODO: should be Element?
class ClassBody (
        val primaryConstructorSignature: PrimaryConstructorSignature?,
        val members: List<Member>,
        val classObjectMembers: List<Member>,
        val lBrace: LBrace,
        val rBrace: RBrace) {

    fun append(builder: CodeBuilder) {
        if (members.isEmpty() && classObjectMembers.isEmpty()) return

        builder append " " append lBrace append "\n"

        builder.append(members, "\n")

        appendClassObject(builder, members.isNotEmpty())

        builder append "\n" append rBrace
    }

    private fun appendClassObject(builder: CodeBuilder, blankLineBefore: Boolean) {
        if (classObjectMembers.isEmpty()) return
        if (blankLineBefore) builder.append("\n\n")
        builder.append(classObjectMembers, "\n", "class object {\n", "\n}")
    }
}
