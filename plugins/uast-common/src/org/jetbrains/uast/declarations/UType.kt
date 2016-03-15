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

interface UType : UElement, UNamed, UFqNamed, UAnnotated, UResolvable, NoTraverse {
    override fun matchesName(name: String) = this.name == name || this.name.endsWith(".$name")

    val isInt: Boolean
    val isBoolean: Boolean

    override fun logString() = "UType ($name)"
    override fun renderString() = name

    override fun resolve(context: UastContext): UClass?
    override fun resolveOrEmpty(context: UastContext) = resolve(context) ?: UClassNotResolved
}

interface UTypeReference : UDeclaration, UResolvable, NoTraverse {
    override fun renderString() = ""
    override fun logString() = log("UTypeReference")

    override fun resolve(context: UastContext): UClass?
    override fun resolveOrEmpty(context: UastContext) = resolve(context) ?: UClassNotResolved
}

object UastErrorType : UType, NoAnnotations {
    override val isInt = false
    override val parent = null
    override val name = "<error>"
    override val fqName = null
    override val isBoolean = false
    override fun resolve(context: UastContext) = null
}