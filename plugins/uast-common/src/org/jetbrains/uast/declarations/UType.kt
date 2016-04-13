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
 * Represents the type.
 * The abstraction is quite simple. Intersection types, union types, platform types are yet to be supported.
 */
interface UType : UElement, UNamed, UFqNamed, UAnnotated, UResolvable {
    /**
     * Returns the simple (non-qualified) type name.
     * The simple type name is only for the debug purposes. Do not check against it in the production code.
     */
    override val name: String

    /**
     * Returns true if the type is either a boxed or an unboxed [Integer], false otherwise.
     */
    val isInt: Boolean

    /**
     * Returns true if the type is either a boxed or an unboxed [Short], false otherwise.
     */
    val isShort: Boolean

    /**
     * Returns true if the type is either a boxed or an unboxed [Long], false otherwise.
     */
    val isLong: Boolean

    /**
     * Returns true if the type is either a boxed or an unboxed [Float], false otherwise.
     */
    val isFloat: Boolean

    /**
     * Returns true if the type is either a boxed or an unboxed [Double], false otherwise.
     */
    val isDouble: Boolean

    /**
     * Returns true if the type is either a boxed or an unboxed [Character], false otherwise.
     */
    val isChar: Boolean

    /**
     * Returns true if the type is either a boxed or an unboxed [Boolean], false otherwise.
     */
    val isBoolean: Boolean

    /**
     * Returns true if the type is either a boxed or an unboxed [Byte], false otherwise.
     */
    val isByte: Boolean

    override fun logString() = "UType ($name)"
    override fun renderString() = name

    /**
     * Returns the [UClass] declaration element for this type.
     *
     * @param context the Uast context
     * @return the [UClass] declaration element, or null if the class was not resolved.
     */
    override fun resolve(context: UastContext): UClass?

    override fun resolveOrEmpty(context: UastContext) = resolve(context) ?: UClassNotResolved

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitType(this)) return
        annotations.acceptList(visitor)
        visitor.afterVisitType(this)
    }
}

/**
 * Represents a type reference.
 */
interface UTypeReference : UDeclaration, UResolvable {
    override fun renderString() = ""
    override fun logString() = log("UTypeReference")

    /**
     * Returns the [UClass] declaration for this type reference.
     *
     * @param context the Uast context
     * @return the [UClass] declaration element, or null if the class was not resolved.
     */
    override fun resolve(context: UastContext): UClass?

    override fun resolveOrEmpty(context: UastContext) = resolve(context) ?: UClassNotResolved
}

object UastErrorType : UType, NoAnnotations {
    override val isInt = false
    override val isLong = false
    override val isShort = false
    override val isFloat = false
    override val isDouble = false
    override val isChar = false
    override val isByte = false
    override val parent = null
    override val name = ERROR_NAME
    override val fqName = null
    override val isBoolean = false
    override fun resolve(context: UastContext) = null
}