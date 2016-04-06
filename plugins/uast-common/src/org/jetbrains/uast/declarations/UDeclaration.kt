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
 * Represents a declaration.
 */
interface UDeclaration : UElement, UNamed {
    /**
     * Returns an element for the name node, or null if the node does not exist in the underlying AST (Psi).
     */
    val nameElement: UElement?

    /**
     * Checks if the function name is [name], and the function containing class qualified name is [containingClassFqName].
     *
     * @param containingClassFqName the required containing class qualified name.
     * @param name the function name to check against.
     * @return true if the call is a function call, the function name is [name],
     *              and the qualified name of the function direct containing class is [containingClassFqName],
     *         false otherwise.
     */
    open fun matchesNameWithContaining(containingClassFqName: String, name: String): Boolean {
        if (!matchesName(name)) return false
        val containingClass = this.getContainingClass() ?: return false
        return containingClass.matchesFqName(containingClassFqName)
    }

    override fun accept(visitor: UastVisitor) {
        visitor.visitElement(this)
        visitor.afterVisitElement(this)
    }
}

object UDeclarationNotResolved : UDeclaration {
    override val name = ERROR_NAME
    override val nameElement = null
    override val parent = null

    override fun logString() = "[!] $name"
    override fun renderString() = name
}