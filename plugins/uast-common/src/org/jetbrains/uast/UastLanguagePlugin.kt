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
 * Interface for the Uast element converter.
 * Each [UastLanguagePlugin] should implement a proper [UastContext],
 *  which translates language-specific AST elements to Uast elements.
 */
interface UastConverter {
    /**
     * Convert a language-specific AST element to the [UElement] implementation with the given parent.
     *
     * @param element the element of the supported language-specific AST.
     * @param parent the parent of the newly created [UElement].
     * @return a [UElement], or null if the given [element] is not supported by this converter.
     *
     * No exceptions should be thrown during conversion, return `null` instead.
     */
    fun convert(element: Any?, parent: UElement): UElement?

    /**
     * Convert [element] to the [UElement] with the given parent.
     */
    fun convertWithParent(element: Any?): UElement?

    /**
     * Checks if the file with the given [name] is supported.
     *
     * @param name the source file name.
     * @return true, if the file is supported by this converter, false otherwise.
     */
    fun isFileSupported(name: String): Boolean
}

/**
 * Interface for the Uast language plugin.
 *
 * [UElement] implementations are loaded using language plugins provided in [UastContext].
 * Each plugin must have a language-specific AST -> Uast (e.g. PsiElement -> UElement) converter.
 */
interface UastLanguagePlugin {
    /**
     * Returns converter for the specific language AST.
     */
    val converter: UastConverter

    /**
     * Returns list of visitor extensions for the specific language AST.
     */
    val visitorExtensions: List<UastVisitorExtension>
}

/**
 * Interface for the Uast visitor extension.
 *
 * A language plugin could provide a number of visitor extensions, which transforms some [UElement] into another.
 * For example, Java accessors (getters/setters) are expressed in Kotlin as properties,
 *  but it can be useful to see them as functions in diagnostics, so the visitor extension
 *  can convert such property accessor calls to the ordinary function calls.
 */
interface UastVisitorExtension {
    /**
     * Invoke extension on an element.
     *
     * @param element a [UElement] to process.
     * @param visitor current visitor.
     * @param context current context.
     */
    operator fun invoke(element: UElement, visitor: UastVisitor, context: UastContext)
}

object UastConverterUtils {
    @JvmStatic
    fun isFileSupported(converters: List<UastLanguagePlugin>, name: String): Boolean {
        return converters.any { it.converter.isFileSupported(name) }
    }
}