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

import org.jetbrains.kotlin.resolve.constants.ConstantValue
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.CacheImplementation.*
import kotlinx.android.extensions.ContainerOptions
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidContainerType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

class ContainerOptionsProxy(val classType: AndroidContainerType, val cache: CacheImplementation) {
    companion object {
        private val CONTAINER_OPTIONS_FQNAME = FqName(ContainerOptions::class.java.canonicalName)
        private val CACHE_NAME = ContainerOptions::cache.name

        private val DEFAULT_CACHE_IMPL = HASH_MAP

        fun get(container: ClassDescriptor): ContainerOptionsProxy {
            val classType = AndroidContainerType.get(container)

            val anno = container.annotations.findAnnotation(CONTAINER_OPTIONS_FQNAME)

            if (anno == null) {
                // Java classes (and Kotlin classes from other modules) does not support cache by default
                val supportsCache = container.source is KotlinSourceElement && classType.doesSupportCache
                return ContainerOptionsProxy(classType, if (supportsCache) DEFAULT_CACHE_IMPL else NO_CACHE)
            }

            val cache = anno.getEnumValue(CACHE_NAME, HASH_MAP) { valueOf(it) }

            return ContainerOptionsProxy(classType, cache)
        }
    }
}

private operator fun AnnotationDescriptor.get(name: String): ConstantValue<*>? {
    return allValueArguments.entries.firstOrNull { it.key.name.asString() == name }?.value
}

private fun <E: Enum<E>> AnnotationDescriptor.getEnumValue(name: String, defaultValue: E, factory: (String) -> E): E {
    val valueName = (this[name] as? EnumValue)?.value?.name?.asString() ?: defaultValue.name

    return try {
        factory(valueName)
    } catch (e: Exception) {
        defaultValue
    }
}