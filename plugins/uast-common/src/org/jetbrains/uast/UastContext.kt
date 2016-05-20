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

/**
 * Interface for the Uast context.
 *
 * Context is needed for resolution, cause the reference can point to the element of the different language.
 */
interface UastContext {
    /**
     * Returns all active language plugins.
     */
    val languagePlugins: List<UastLanguagePlugin>

    /**
     * Convert an element of some language-specific AST to Uast element.
     * If two or more language plugins can convert the given [element] type,
     *  the first converter in the [languagePlugins] list will be chosen.
     *
     * @param element the language-specific AST element
     * @return [UElement] instance, or null if [element] type is not supported by any of the provided language plugins.
     */
    fun convert(element: Any?): UElement? {
        if (element == null) {
            return null
        }

        for (plugin in languagePlugins) {
            val uelement = plugin.converter.convertWithParent(element)
            if (uelement != null) {
                return uelement
            }
        }
        return null
    }
}