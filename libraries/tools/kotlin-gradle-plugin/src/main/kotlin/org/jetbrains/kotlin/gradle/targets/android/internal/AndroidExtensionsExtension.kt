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

package org.jetbrains.kotlin.gradle.internal

import java.util.*

enum class CacheImplementation(val optionName: String) {
    HASH_MAP("hashMap"),
    SPARSE_ARRAY("sparseArray"),
    NONE("none")
}

enum class AndroidExtensionsFeature(val featureName: String) {
    VIEWS("views"),
    PARCELIZE("parcelize");

    internal companion object {
        internal fun parseFeatures(features: Set<String>): SortedSet<AndroidExtensionsFeature> {
            fun find(name: String) = AndroidExtensionsFeature.values().firstOrNull { it.featureName == name }
                ?: error("Can't find Android Extensions feature $name")
            return features.mapTo(sortedSetOf()) { find(it) }
        }
    }
}

open class AndroidExtensionsExtension {
    open var isExperimental: Boolean = false

    open var features: Set<String> = AndroidExtensionsFeature.values().mapTo(mutableSetOf()) { it.featureName }

    open var defaultCacheImplementation: CacheImplementation = CacheImplementation.HASH_MAP
}