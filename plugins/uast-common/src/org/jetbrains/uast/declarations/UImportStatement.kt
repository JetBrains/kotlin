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
 * Represents an import statement.
 */
interface UImportStatement : UElement {
    /**
     * Returns the qualified name to import.
     */
    val fqNameToImport: String?

    /**
     * Returns true is the import is a "star" import (on-demand, all-under).
     */
    val isStarImport: Boolean

    /**
     * Resolve the import statement to the declaration.
     *
     * @param context the Uast context
     * @return the declaration element, or null if the declaration was not resolved.
     */
    fun resolve(context: UastContext): UDeclaration?

    override fun accept(visitor: UastVisitor) {
        visitor.visitImportStatement(this)
        visitor.afterVisitImportStatement(this)
    }

    override fun logString() = "UImport ($fqNameToImport)"
    override fun renderString() = "import ${fqNameToImport ?: ""}"
}