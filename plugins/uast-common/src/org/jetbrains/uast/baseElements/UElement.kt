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

interface UElement {
    val parent: UElement?
    fun logString(): String
    fun renderString(): String = logString()
    fun traverse(handler: UastHandler)
}

interface UNamed {
    val name: String

    fun matchesName(name: String) = this.name == name
}

interface UFqNamed : UNamed {
    val fqName: String?

    val fqNameOrName: String
        get() = fqName ?: name

    fun matchesFqName(fqName: String) = this.fqName == fqName
}

interface UModifierOwner {
    fun hasModifier(modifier: UastModifier): Boolean
}

interface NoTraverse : UElement {
    override fun traverse(handler: UastHandler) {}
}

interface UResolvable {
    fun resolve(context: UastContext): UDeclaration?
    fun resolveOrEmpty(context: UastContext): UDeclaration = resolve(context) ?: UDeclarationNotResolved

    fun resolveClass(context: UastContext): UClass? = resolve(context) as? UClass
}