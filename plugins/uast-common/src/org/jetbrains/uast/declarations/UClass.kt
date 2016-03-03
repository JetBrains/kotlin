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

interface UClass : UDeclaration, UFqNamed, UModifierOwner, UAnnotated {
    val isEnum: Boolean
    val isInterface: Boolean
    val isObject: Boolean
    val isAnonymous: Boolean
    val isAnnotation: Boolean
    val visibility: UastVisibility

    val internalName: String?

    val superTypes: List<UType>
    val declarations: List<UDeclaration>

    val nestedClasses: List<UClass>
        get() = declarations.filterIsInstance<UClass>()

    val functions: List<UFunction>
        get() = declarations.filterIsInstance<UFunction>()

    val properties: List<UVariable>
        get() = declarations.filterIsInstance<UVariable>()

    @Suppress("UNCHECKED_CAST")
    val constructors: List<UFunction>
        get() = declarations.filter { it is UFunction && it.kind == UastFunctionKind.CONSTRUCTOR } as List<UFunction>

    fun isSubclassOf(name: String) : Boolean

    fun getSuperClass(context: UastContext): UClass?

    override fun traverse(handler: UastHandler) {
        nameElement?.handleTraverse(handler)
        declarations.handleTraverseList(handler)
        annotations.handleTraverseList(handler)
    }

    override fun renderString(): String {
        val keyword = when {
            isEnum -> "enum"
            isInterface -> "interface"
            isObject -> "object"
            else -> "class"
        }

        val modifiers = listOf(UastModifier.ABSTRACT, UastModifier.FINAL, UastModifier.INNER)
                .filter { hasModifier(it) }.joinToString(" ") { it.name }.let { if (it.isBlank()) it else "$it " }

        val name = if (isAnonymous) "" else " $name"

        val declarations = if (declarations.isEmpty()) "" else buildString {
            appendln("{")
            append(declarations.joinToString("\n") { it.renderString() }.withMargin)
            append("\n}")
        }

        return "${visibility.name} " + modifiers + keyword + name + " " + declarations
    }

    override fun logString() = "UClass ($name, enum = $isEnum, interface = $isInterface, object = $isObject)\n" + declarations.logString()
}

object UClassNotResolved : UClass {
    override val isEnum = false
    override val isInterface = false
    override val isObject = false
    override val isAnonymous = true
    override val isAnnotation = false
    override val visibility = UastVisibility.PRIVATE
    override val superTypes = emptyList<UType>()
    override val declarations = emptyList<UDeclaration>()
    override fun isSubclassOf(name: String) = false

    override val nameElement = null
    override val parent = null
    override val name = "<class not resolved>"
    override val fqName = null
    override val internalName = null

    override fun hasModifier(modifier: UastModifier) = false
    override val annotations = emptyList<UAnnotation>()

    override fun getSuperClass(context: UastContext) = null
}