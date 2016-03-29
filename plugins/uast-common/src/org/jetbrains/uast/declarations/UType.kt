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

interface UType : UElement, UNamed, UFqNamed, UAnnotated, UResolvable, LeafUElement {
    override fun matchesName(name: String) = this.name == name || this.name.endsWith(".$name")

    /* The simple type name is only for the debug purposes. Do not check against it in the production code */
    override val name: String

    /* Semantics: returns true if the type is either a boxed or an unboxed, false otherwise */
    val isInt: Boolean
    val isLong: Boolean
    val isFloat: Boolean
    val isDouble: Boolean
    val isChar: Boolean
    val isBoolean: Boolean
    val isByte: Boolean

    override fun logString() = "UType ($name)"
    override fun renderString() = name

    override fun resolve(context: UastContext): UClass?
    override fun resolveOrEmpty(context: UastContext) = resolve(context) ?: UClassNotResolved

    override fun traverse(callback: UastCallback) {
        annotations.handleTraverseList(callback)
    }
}

interface UTypeReference : UDeclaration, UResolvable, LeafUElement {
    override fun renderString() = ""
    override fun logString() = log("UTypeReference")

    override fun resolve(context: UastContext): UClass?
    override fun resolveOrEmpty(context: UastContext) = resolve(context) ?: UClassNotResolved
}

object UastErrorType : UType, NoAnnotations {
    override val isInt = false
    override val isLong = false
    override val isFloat = false
    override val isDouble = false
    override val isChar = false
    override val isByte = false
    override val parent = null
    override val name = "<error>"
    override val fqName = null
    override val isBoolean = false
    override fun resolve(context: UastContext) = null
}