/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.annotation

import java.io.File
import java.io.Reader
import java.io.StringReader

class KotlinAnnotationProvider(annotationsReader: Reader) {

    private companion object {
        const val ANNOTATED_CLASS = "c"
        const val ANNOTATED_METHOD = "m"
        const val ANNOTATED_FIELD = "f"

        const val SHORTENED_ANNOTATION = "a"
        const val SHORTENED_PACKAGE_NAME = "p"

        const val CLASS_DECLARATION = "d"
    }

    constructor(annotationsFile: File) : this(annotationsFile.reader().buffered())
    constructor() : this(StringReader(""))

    private val kotlinClassesInternal = hashSetOf<String>()
    private val annotatedKotlinElementsInternal = hashMapOf<String, MutableSet<AnnotatedElementDescriptor>>()

    init {
        readAnnotations(annotationsReader)
    }

    val annotatedKotlinElements: Map<String, Set<AnnotatedElementDescriptor>>
        get() = annotatedKotlinElementsInternal

    val kotlinClasses: Set<String>
        get() = kotlinClassesInternal

    val supportInheritedAnnotations: Boolean
        get() = kotlinClassesInternal.isNotEmpty()

    private fun readAnnotations(annotationsReader: Reader) {
        fun handleShortenedName(cache: MutableMap<String, String>, lineParts: List<String>) {
            val name = lineParts[1]
            val id = lineParts[2]
            cache.put(id, name)
        }

        val shortenedAnnotationCache = hashMapOf<String, String>()
        val shortenedPackageNameCache = hashMapOf<String, String>()

        fun expandAnnotation(s: String) = shortenedAnnotationCache.getOrElse(s) { s }

        fun expandClassName(s: String): String {
            val id = s.substringBefore('/', "")
            if (id.isEmpty()) return s
            val shortenedValue = shortenedPackageNameCache.get(id) ?:
                    throw RuntimeException("Value for $id couldn't be found in shrink cache")

            return shortenedValue + '.' + s.substring(id.length + 1)
        }

        annotationsReader.useLines { lines ->
            for (line in lines) {
                if (line.isEmpty()) continue
                val lineParts = line.split(' ')

                val type = lineParts[0]
                when (type) {
                    SHORTENED_ANNOTATION -> handleShortenedName(shortenedAnnotationCache, lineParts)
                    SHORTENED_PACKAGE_NAME -> handleShortenedName(shortenedPackageNameCache, lineParts)
                    CLASS_DECLARATION -> {
                        val classFqName = expandClassName(lineParts[1]).replace('$', '.')
                        kotlinClassesInternal.add(classFqName)
                    }

                    ANNOTATED_CLASS, ANNOTATED_FIELD, ANNOTATED_METHOD -> {
                        val annotationName = expandAnnotation(lineParts[1])
                        val classFqName = expandClassName(lineParts[2]).replace('$', '.')
                        val elementName = if (lineParts.size == 4) lineParts[3] else null

                        val set = annotatedKotlinElementsInternal.getOrPut(annotationName) { hashSetOf() }
                        set.add(when (type) {
                            ANNOTATED_CLASS -> AnnotatedElementDescriptor.Class(classFqName)
                            ANNOTATED_FIELD -> {
                                val name = elementName ?: throw AssertionError("Name for field must be provided")
                                AnnotatedElementDescriptor.Field(classFqName, name)
                            }
                            ANNOTATED_METHOD -> {
                                val name = elementName ?: throw AssertionError("Name for method must be provided")

                                if ("<init>" == name)
                                    AnnotatedElementDescriptor.Constructor(classFqName)
                                else
                                    AnnotatedElementDescriptor.Method(classFqName, name)
                            }
                            else -> throw AssertionError("Unknown type: $type")
                        })

                    }
                    else -> throw AssertionError("Unknown type: $type")
                }
            }
        }
    }
}