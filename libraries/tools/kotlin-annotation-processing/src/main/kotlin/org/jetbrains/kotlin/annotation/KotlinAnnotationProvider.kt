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

import javax.tools.FileObject
import kotlin.properties.Delegates

public abstract class KotlinAnnotationProvider {

    private companion object {
        val ANNOTATED_CLASS = "c"
        val ANNOTATED_METHOD = "m"
        val ANNOTATED_FIELD = "f"

        val SHORTENED_ANNOTATION = "a"
        val SHORTENED_PACKAGE_NAME = "p"
    }

    public val annotatedKotlinElements: MutableMap<String, MutableSet<AnnotatedElementDescriptor>> by Delegates.lazy {
        readAnnotations()
    }

    protected abstract val serializedAnnotations: String

    private fun readAnnotations(): MutableMap<String, MutableSet<AnnotatedElementDescriptor>> {
        val shortenedAnnotationCache = hashMapOf<String, String>()
        val shortenedPackageNameCache = hashMapOf<String, String>()

        fun expandAnnotation(s: String) = shortenedAnnotationCache.getOrElse(s) { s }

        fun expandClassName(s: String): String {
            val id = s.substringBefore('/', "")
            if (id.isEmpty()) return s
            val shortenedValue = shortenedPackageNameCache.get(id) ?:
                    throw RuntimeException("Value for $id couldn't be found in shrink cache")

            return shortenedValue + '.' + s.substring(id.length() + 1)
        }

        val annotatedKotlinElements: MutableMap<String, MutableSet<AnnotatedElementDescriptor>> = hashMapOf()

        for (line in serializedAnnotations.split('\n')) {
            if (line.isEmpty()) continue
            val lineParts = line.split(' ')

            val type = lineParts[0]
            when (type) {
                SHORTENED_ANNOTATION -> handleShortenedName(shortenedAnnotationCache, lineParts)
                SHORTENED_PACKAGE_NAME -> handleShortenedName(shortenedPackageNameCache, lineParts)

                ANNOTATED_CLASS, ANNOTATED_FIELD, ANNOTATED_METHOD -> {
                    val annotationName = expandAnnotation(lineParts[1])
                    val classFqName = expandClassName(lineParts[2])
                    val elementName = if (lineParts.size() == 4) lineParts[3] else null

                    val set = annotatedKotlinElements.getOrPut(annotationName) { hashSetOf() }
                    set.add(when (type) {
                        ANNOTATED_CLASS -> AnnotatedClassDescriptor(classFqName)
                        ANNOTATED_FIELD -> AnnotatedFieldDescriptor(classFqName, elementName!!)
                        ANNOTATED_METHOD -> AnnotatedMethodDescriptor(classFqName, elementName!!)
                        else -> throw AssertionError("Should not occur")
                    })

                }
                else -> throw RuntimeException("Unknown type: $type")
            }
        }

        return annotatedKotlinElements
    }

    private fun handleShortenedName(cache: MutableMap<String, String>, lineParts: List<String>) {
        val name = lineParts[1]
        val id = lineParts[2]
        cache.put(id, name)
    }

}

public class FileObjectKotlinAnnotationProvider(val annotationsFileObject: FileObject): KotlinAnnotationProvider() {
    override val serializedAnnotations: String
        get() = annotationsFileObject.getCharContent(false).toString()
}