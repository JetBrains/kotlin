/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
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