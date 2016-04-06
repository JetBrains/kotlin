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

interface UastConverter {
    fun convert(element: Any?, parent: UElement): UElement?
    fun convertWithParent(element: Any?): UElement?

    fun isFileSupported(name: String): Boolean
}

interface UastLanguagePlugin {
    val converter: UastConverter
    val visitorExtensions: List<UastVisitorExtension>
}

interface UastVisitorExtension {
    operator fun invoke(element: UElement, visitor: UastVisitor, context: UastContext)
}

object UastConverterUtils {
    @JvmStatic
    fun isFileSupported(converters: List<UastLanguagePlugin>, name: String): Boolean {
        return converters.any { it.converter.isFileSupported(name) }
    }
}