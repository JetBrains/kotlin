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

/**
 * Represents a function.
 * Function could be a JVM method, a property accessor, a constructor, etc.
 */
interface UFunction : UDeclaration, UModifierOwner, UVisibilityOwner, UAnnotated {
    /**
     * Returns the function kind.
     */
    val kind: UastFunctionKind

    /**
     * Returns the function value parameters.
     */
    val valueParameters: List<UVariable>

    /**
     * Returns the function value parameters count.
     * Retrieving the parameter count could be faster than getting the [valueParameters.size],
     *    because there is no need to create actual [UVariable] instances.
     */
    val valueParameterCount: Int

    /**
     * Returns the function type parameters.
     */
    val typeParameters: List<UTypeReference>

    /**
     * Returns the function type parameter count.
     */
    val typeParameterCount: Int

    /**
     * Returns the function return type, or null if the function does not have a return type
     *     (e.g. it is a constructor).
     */
    val returnType: UType?

    /**
     * Returns the function body expression.
     */
    val body: UExpression?

    /**
     * Returns the function JVM descriptor (for example, "(ILjava/lang/String;)[I"), or null if the descriptor is unknown.
     */
    open val bytecodeDescriptor: String?
        get() = null


    /**
     * Get the list of all super functions for this function.
     */
    fun getSuperFunctions(context: UastContext): List<UFunction>

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitFunction(this)) return
        nameElement?.accept(visitor)
        valueParameters.acceptList(visitor)
        body?.accept(visitor)
        annotations.acceptList(visitor)
        typeParameters.acceptList(visitor)
        returnType?.accept(visitor)
        visitor.afterVisitFunction(this)
    }

    override fun renderString(): String = buildString {
        appendWithSpace(visibility.name)
        appendWithSpace(renderModifiers())
        append("fun ")
        if (typeParameterCount > 0) {
            append('<').append(typeParameters.joinToString { it.renderString() }).append("> ")
        }
        append(name)
        append('(')
        append(valueParameters.joinToString() { it.renderString() })
        append(')')
        returnType?.let { append(": " + it.renderString()) }

        val body = body
        val bodyRendered = when (body) {
            null -> ""
            is UBlockExpression -> " " + body.renderString()
            else -> " = " + body.renderString()
        }
        append(bodyRendered)
    }

    override fun logString() = log("UFunction ($name, kind = ${kind.text}, paramCount = $valueParameterCount)", body)
}

object UFunctionNotResolved : UFunction {
    override val kind = UastFunctionKind(ERROR_NAME)
    override val valueParameters = emptyList<UVariable>()
    override val valueParameterCount = 0
    override val typeParameters = emptyList<UTypeReference>()
    override val typeParameterCount = 0
    override val returnType = null
    override val body = null
    override val visibility = UastVisibility.PRIVATE
    override val nameElement = null
    override val parent = null
    override val name = ERROR_NAME

    override fun hasModifier(modifier: UastModifier) = false
    override fun getSuperFunctions(context: UastContext) = emptyList<UFunction>()
    override val annotations = emptyList<UAnnotation>()
}