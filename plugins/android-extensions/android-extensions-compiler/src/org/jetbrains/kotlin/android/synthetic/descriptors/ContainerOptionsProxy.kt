/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.synthetic.descriptors

import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.CacheImplementation.NO_CACHE
import kotlinx.android.extensions.CacheImplementation.valueOf
import kotlinx.android.extensions.ContainerOptions
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidContainerType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

class ContainerOptionsProxy(val containerType: AndroidContainerType, val cache: CacheImplementation?) {
    companion object {
        private val CONTAINER_OPTIONS_FQNAME = FqName(ContainerOptions::class.java.canonicalName)
        private val CACHE_NAME = ContainerOptions::cache.name

        fun create(container: ClassDescriptor): ContainerOptionsProxy {
            if (container.kind != ClassKind.CLASS && container.kind != ClassKind.INTERFACE) {
                return ContainerOptionsProxy(AndroidContainerType.UNKNOWN, NO_CACHE)
            }

            val containerType = AndroidContainerType.get(container)

            val anno = container.annotations.findAnnotation(CONTAINER_OPTIONS_FQNAME)

            if (anno == null) {
                // Java classes (and Kotlin classes from other modules) does not support cache by default
                val supportsCache = container.kind == ClassKind.CLASS
                                    && container.source is KotlinSourceElement
                                    && containerType.doesSupportCache

                return ContainerOptionsProxy(
                        containerType,
                        if (supportsCache) null else NO_CACHE) // `null` here means "use global cache implementation setting"
            }

            val cache = anno.getEnumValue(CACHE_NAME) { valueOf(it) }

            return ContainerOptionsProxy(containerType, cache)
        }
    }
}

private fun <E : Enum<E>> AnnotationDescriptor.getEnumValue(name: String, factory: (String) -> E): E? {
    val valueName = (allValueArguments[Name.identifier(name)] as? EnumValue)?.enumEntryName?.asString() ?: return null

    return try {
        factory(valueName)
    }
    catch (e: IllegalArgumentException) {
        // Enum.valueOf() may throw this
        null
    }
}
