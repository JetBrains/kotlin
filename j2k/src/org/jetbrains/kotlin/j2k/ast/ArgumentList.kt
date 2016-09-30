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
import org.jetbrains.kotlin.j2k.append

class ArgumentList(
        val expressions: List<Expression>,
        val lPar: LPar,
        val rPar: RPar
) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(lPar)
        builder.append(expressions, ", ")
        builder.append(rPar)
    }

    companion object {
        fun withNoPrototype(arguments: List<Expression>): ArgumentList {
            return ArgumentList(arguments, LPar.withPrototype(null), RPar.withPrototype(null)).assignNoPrototype()
        }

        fun withNoPrototype(vararg arguments: Expression): ArgumentList = withNoPrototype(arguments.asList())
    }
}
