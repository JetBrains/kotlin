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
 * Represents a source file.
 * File is the topmost element of the UElement hierarchy.
 * Files should not be nested, thus the [parent] property should always return null.
 */
interface UFile: UElement {
    /**
     * Returns the qualified package name of this file.
     * Could be an empty string if the package is "default", or null if the package directive is not present in this file.
     */
    val packageFqName: String?

    /**
     * Returns the list of import statements.
     */
    val importStatements: List<UImportStatement>

    /**
     * Returns the list of declarations in this file (classes, properties, functions, etc).
     */
    val declarations: List<UDeclaration>

    /**
     * Returns list of classes containing in this file.
     */
    val classes: List<UClass>
        get() = declarations.filterIsInstance<UClass>()

    override val parent: UElement?
        get() = null

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitFile(this)) return
        declarations.acceptList(visitor)
        importStatements.acceptList(visitor)
        visitor.afterVisitFile(this)
    }

    override fun logString() = "UFile (package = $packageFqName)\n" + declarations.joinToString("\n") { it.logString().withMargin }

    override fun renderString() = buildString {
        if (!packageFqName.isNullOrBlank()) {
            appendln("package $packageFqName")
            appendln()
        }

        declarations.forEach { appendln(it.renderString()) }
    }
}
