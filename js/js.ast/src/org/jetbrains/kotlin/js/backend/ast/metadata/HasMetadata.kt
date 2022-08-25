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

package org.jetbrains.kotlin.js.backend.ast.metadata

interface HasMetadata {
    fun <T> getData(key: String): T
    fun <T> setData(key: String, value: T)
    fun hasData(key: String): Boolean
    fun removeData(key: String)
    fun copyMetadataFrom(other: HasMetadata)
    fun getRawMetadata(): Map<String, Any?>
}

open class HasMetadataImpl : HasMetadata {
    private var metadataImpl: MutableMap<String, Any?>? = null

    private val metadata: MutableMap<String, Any?>
        get() = metadataImpl ?: hashMapOf<String, Any?>().also { metadataImpl = it }

    final override fun <T> getData(key: String): T {
        @Suppress("UNCHECKED_CAST") return metadata[key] as T
    }

    final override fun <T> setData(key: String, value: T) {
        metadata[key] = value
    }

    final override fun hasData(key: String): Boolean {
        return metadataImpl?.containsKey(key) == true
    }

    final override fun removeData(key: String) {
        metadataImpl?.remove(key)
    }

    final override fun copyMetadataFrom(other: HasMetadata) {
        val otherRawMetadata = other.getRawMetadata()
        if (otherRawMetadata.isNotEmpty()) {
            metadata.putAll(otherRawMetadata)
        }
    }

    final override fun getRawMetadata(): Map<String, Any?> {
        return metadataImpl ?: emptyMap()
    }
}
