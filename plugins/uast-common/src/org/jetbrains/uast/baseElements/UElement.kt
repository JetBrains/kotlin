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
 * The common interface for all Uast elements.
 */
interface UElement {
    /**
     * Returns the element parent.
     */
    val parent: UElement?

    /**
     * Returns true if this element is valid, false otherwise.
     */
    open val isValid: Boolean
        get() = true

    /**
     * Returns the log string.
     *
     * Output example (should be something like this):
     * UWhileExpression
     *     UBinaryExpression (>)
     *         USimpleReferenceExpression (i)
     *         ULiteralExpression (5)
     *     UBlockExpression
     *         UCallExpression (println)
     *             ULiteralExpression (ABC)
     *         UPostfixExpression (--)
     *             USimpleReferenceExpression(i)
     *
     * @return the expression tree for this element.
     * @see [UIfExpression] for example.
     */
    fun logString(): String

    /**
     * Returns the string in pseudo-code.
     *
     * Output example (should be something like this):
     * while (i > 5) {
     *     println("Hello, world")
     *     i--
     * }
     *
     * @return the rendered text.
     * @see [UIfExpression] for example.
     */
    fun renderString(): String = logString()

    /**
     * Passes the element to the specified visitor.
     *
     * @param visitor the visitor to pass the element to.
     */
    fun accept(visitor: UastVisitor) {
        visitor.visitElement(this)
    }
}

/**
 * An interface for the [UElement] which has a name.
 */
interface UNamed {
    /**
     * Returns the name of this element.
     */
    val name: String

    /**
     * Checks if the element name is equal to the passed name.
     *
     * @param name the name to check against
     * @return true if the element name is equal to [name], false otherwise.
     */
    fun matchesName(name: String) = this.name == name
}

/**
 * An interface for the [UElement] which has a qualified name.
 */
interface UFqNamed : UNamed {
    /**
     * Returns the qualified name of this element, or null if the qualified name was not resolved.
     */
    val fqName: String?

    /**
     * Checks if the element qualified name is equal to the passed name.
     *
     * @param fqName qualified name to check against
     * @return true if the element name is not null and is equal to [name], false otherwise.
     */
    fun matchesFqName(fqName: String) = this.fqName == fqName
}

/**
 * An interface for the [UElement] which has Uast modifiers.
 */
interface UModifierOwner {
    /**
     * Checks if the element has the passed modifier.
     *
     * @param modifier modifier to check
     * @return true if the element has [modifier], false otherwise.
     */
    fun hasModifier(modifier: UastModifier): Boolean
}

/**
 * An interface for the [UElement] which has a visibility (class, function, variable).
 */
interface UVisibilityOwner {
    /**
     * Returns the element visibility.
     */
    val visibility: UastVisibility
}

/**
 * An inteface for the [UElement] which can be resolved to the its declaration.
 */
interface UResolvable {
    /**
     * Returns the declaration element.
     *
     * @param context the Uast context
     * @return the resolved declaration, or null if the declaration was not resolved.
     */
    fun resolve(context: UastContext): UDeclaration?

    /**
     * Returns the declaration element or an empty Uast declaration element if the declaration was not resolved.
     * An empty declaration element should be a singleton.
     * [resolveOrEmpty] should not create new elements each time the declaration was not resolved.
     *
     * @param context the Uast context
     * @return the resolved declaration, of an empty error Uast declaration element if the declaration was not resolved.
     *
     * @see [UDeclarationNotResolved]
     */
    fun resolveOrEmpty(context: UastContext): UDeclaration = resolve(context) ?: UDeclarationNotResolved

    /**
     * Returns the declaration [UClass] element of null if the declaration was not resolved.
     *
     * @param context the Uast context
     * @return the resolved [UClass] element, or null if the class was not resolved,
     *         or if the resolved [UElement] is not an instance of [UClass].
     */
    fun resolveClass(context: UastContext): UClass? = resolve(context) as? UClass
}