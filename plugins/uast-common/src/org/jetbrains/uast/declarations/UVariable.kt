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

interface UVariable : UDeclaration, UModifierOwner, UVisibilityOwner, UAnnotated {
    /**
     * Return the variable initializer (or the default value for value parameter), or null if the variable is not initialized.
     */
    val initializer: UExpression?

    /**
     * Return the variable kind.
     */
    val kind: UastVariableKind

    /**
     * Return the variable type.
     */
    val type: UType

    /**
     * Return the list of accessors if the variable is a property, or null otherwise.
     */
    open val accessors: List<UFunction>?
        get() = null

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitVariable(this)) return
        nameElement?.accept(visitor)
        initializer?.accept(visitor)
        annotations.acceptList(visitor)
        type.accept(visitor)
        visitor.afterVisitVariable(this)
    }

    override fun renderString(): String = buildString {
        if (kind != UastVariableKind.VALUE_PARAMETER) appendWithSpace(visibility.name)
        appendWithSpace(renderModifiers())
        if (kind != UastVariableKind.VALUE_PARAMETER) append("var ")
        append(name)
        append(": ")
        append(type.name)
        if (initializer != null && initializer !is EmptyUExpression) {
            append(" = ")
            append(initializer!!.renderString())
        }

        accessors?.let {
            appendln()
            it.forEachIndexed { i, accessor ->
                this@buildString.append(accessor.renderString().withMargin)
                if ((i + 1) < it.size) appendln()
            }
        }
    }

    override fun logString() = "UVariable ($name, kind = ${kind.name})\n" +
            (initializer?.let { it.logString().withMargin } ?: "<no initializer>")
}

object UVariableNotResolved : UVariable {
    override val initializer = null
    override val kind = UastVariableKind(ERROR_NAME)
    override val type = UastErrorType
    override val nameElement = null
    override val parent = null
    override val name = ERROR_NAME
    override val visibility = UastVisibility.LOCAL

    override fun hasModifier(modifier: UastModifier) = false
    override val annotations = emptyList<UAnnotation>()
}