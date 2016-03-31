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

import org.jetbrains.uast.visitor.UastVisitor

interface UVariable : UDeclaration, UModifierOwner, UAnnotated {
    val initializer: UExpression?
    val kind: UastVariableKind
    val type: UType
    val visibility: UastVisibility

    open val getters: List<UFunction>?
        get() = null

    open val setters: List<UFunction>?
        get() = null

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitVariable(this)) return
        nameElement?.accept(visitor)
        initializer?.accept(visitor)
        annotations.acceptList(visitor)
        type.accept(visitor)
    }

    override fun renderString(): String {
        val initializer = if (initializer != null && initializer !is EmptyExpression) " = ${initializer!!.renderString()}" else ""
        val prefix = if (kind == UastVariableKind.VALUE_PARAMETER) "" else "var "
        val emptyLine = if (kind == UastVariableKind.MEMBER) "\n" else ""
        return "$prefix$name: " + type.name + initializer + emptyLine
    }

    override fun logString() = "UVariable ($name, kind = ${kind.name})\n" +
            (initializer?.let { it.logString().withMargin } ?: "<no initializer>")
}

object UVariableNotResolved : UVariable {
    override val initializer = null
    override val kind = UastVariableKind.MEMBER
    override val type = UastErrorType
    override val nameElement = null
    override val parent = null
    override val name = "<variable not resolved>"
    override val visibility = UastVisibility.LOCAL

    override fun hasModifier(modifier: UastModifier) = false
    override val annotations = emptyList<UAnnotation>()
}