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
 * Represents a JVM class (ordinary class, interface, annotation, singleton, etc.).
 */
interface UClass : UDeclaration, UFqNamed, UModifierOwner, UVisibilityOwner, UAnnotated {
    /**
     * Returns the class simple (non-qualified) name.
     * The simple class name is only for the debug purposes. Do not check against it in the production code.
     */
    override val name: String

    /**
     * Returns true if the class is anonymous ([name] in this case should return a placeholder like "<anonymous>").
     */
    val isAnonymous: Boolean

    /**
     * Returns the class kind (ordinary class, interface, annotation, etc.).
     */
    val kind: UastClassKind

    /**
     * Returns the default type for this class.
     */
    val defaultType: UType

    /**
     * Returns the class companions.
     */
    val companions: List<UClass>

    /**
     * Returns the class JVM name, or null if the JVM name is unknown.
     */
    open val internalName: String?
        get() = null

    /**
     * Returns the all supertypes of this class.
     */
    val superTypes: List<UType>

    /**
     * Returns class declarations (nested classes, constructors, functions, variables, etc.).
     * An empty (default) constructor also should be present.
     */
    val declarations: List<UDeclaration>

    /**
     * Returns nested classes declared in this class.
     */
    val nestedClasses: List<UClass>
        get() = declarations.filterIsInstance<UClass>()

    /**
     * Returns functions declared in this class.
     */
    val functions: List<UFunction>
        get() = declarations.filterIsInstance<UFunction>()

    /**
     * Returns properties declared in this class.
     */
    val properties: List<UVariable>
        get() = declarations.filterIsInstance<UVariable>()

    /**
     * Returns constructors declared in this class.
     */
    @Suppress("UNCHECKED_CAST")
    val constructors: List<UFunction>
        get() = declarations.filter { it is UFunction && it.kind == UastFunctionKind.CONSTRUCTOR } as List<UFunction>

    /**
     * Checks if the class is subclass of another.
     *
     * @param fqName qualified name of the class to check against
     * @return true if the class is the *subclass* of the class with the specified [fqName],
     *         false if the class qualified name is [fqName] or if the class is not a subclass of the class with the specified [fqName]
     */
    fun isSubclassOf(fqName: String) : Boolean

    /**
     * Get the direct superclass of this class.
     *
     * @return null if the class has not a superclass (java.lang.Object).
     */
    fun getSuperClass(context: UastContext): UClass?

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitClass(this)) return
        nameElement?.accept(visitor)
        declarations.acceptList(visitor)
        annotations.acceptList(visitor)
        visitor.afterVisitClass(this)
    }

    override fun renderString() = buildString {
        appendWithSpace(visibility.name)
        appendWithSpace(renderModifiers())
        appendWithSpace(kind.text)
        appendWithSpace(name)

        val declarations = if (declarations.isEmpty()) "" else buildString {
            appendln("{")
            append(declarations.joinToString("\n\n") { it.renderString().trim('\n') }.withMargin)
            append("\n}")
        }
        append(declarations)
    }

    override fun logString() = log("UClass ($name, kind = ${kind.text})", declarations)
}

object UClassNotResolved : UClass {
    override val isAnonymous = true
    override val kind = UastClassKind.CLASS
    override val visibility = UastVisibility.PRIVATE
    override val superTypes = emptyList<UType>()
    override val declarations = emptyList<UDeclaration>()
    override fun isSubclassOf(fqName: String) = false
    override val companions = emptyList<UClass>()
    override val defaultType = UastErrorType
    override val nameElement = null
    override val parent = null
    override val name = ERROR_NAME
    override val fqName = null
    override val internalName = null

    override fun hasModifier(modifier: UastModifier) = false
    override val annotations = emptyList<UAnnotation>()

    override fun getSuperClass(context: UastContext) = null
}