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

interface UFile: UElement {
    val packageFqName: String?
    val importStatements: List<UImportStatement>
    val declarations: List<UDeclaration>

    val classes: List<UClass>
        get() = declarations.filterIsInstance<UClass>()

    override val parent: UElement?
        get() = null

    override fun traverse(callback: UastCallback) {
        declarations.handleTraverseList(callback)
        importStatements.handleTraverseList(callback)
    }

    override fun logString() = "UFile (package = $packageFqName)\n" + declarations.joinToString("\n") { it.logString().withMargin }

    override fun renderString() = buildString {
        if (packageFqName != null) {
            appendln("package $packageFqName")
            appendln()
        }

        declarations.forEach { appendln(it.renderString()) }
    }
}
