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

import org.jetbrains.kotlin.j2k.CodeBuilder

class LocalVariable(
        private val identifier: Identifier,
        private val annotations: Annotations,
        private val modifiers: Modifiers,
        private val explicitType: Type?,
        private val initializer: Expression,
        private val isVal: Boolean
) : Element() {

    override fun generateCode(builder: CodeBuilder) {
        if(initializer is AssignmentExpression)
            builder append initializer append "\n"
        builder append annotations append (if (isVal) "val " else "var ") append identifier
        if (explicitType != null) {
            builder append ":" append explicitType
        }
        if (!initializer.isEmpty) {
            builder append " = "
            if(initializer is AssignmentExpression)
                builder append initializer.left
            else
                builder append initializer
        }
    }
}
