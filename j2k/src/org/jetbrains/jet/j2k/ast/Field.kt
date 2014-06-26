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

class Field(
        val identifier: Identifier,
        annotations: Annotations,
        modifiers: Modifiers,
        val `type`: Type,
        val initializer: Element,
        val isVal: Boolean,
        val explicitType: Boolean,
        private val defaultInitializer: Boolean
) : Member(annotations, modifiers) {

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations)
                .appendWithSpaceAfter(modifiers)
                .append(if (isVal) "val " else "var ")
                .append(identifier)

        if (explicitType) {
            builder append ":" append `type`
        }

        var initializerToUse = initializer
        if (initializerToUse.isEmpty && defaultInitializer) {
            initializerToUse = getDefaultInitializer(this)
        }
        if (!initializerToUse.isEmpty) {
            builder append " = " append initializerToUse
        }
    }
}
