/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast

interface UDeclarationsExpression : UExpression {
    val declarations: List<UElement>

    val variables: List<UVariable>
        get() = declarations.filterIsInstance<UVariable>()

    override fun evaluate() = null
    override fun traverse(callback: UastCallback) = declarations.handleTraverseList(callback)

    override fun renderString() = declarations.joinToString("\n") { it.renderString() }
    override fun logString() = log("UDeclarationsExpression", declarations)
}

class SimpleUDeclarationsExpression(
        override val parent: UElement,
        override val declarations: List<UElement>
) : UDeclarationsExpression {
    override fun evaluate() = null
}