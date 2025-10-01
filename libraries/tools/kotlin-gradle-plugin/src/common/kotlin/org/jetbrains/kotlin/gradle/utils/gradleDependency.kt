/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.Dependency

/**
 * Convert a [Dependency] to a GAV string.
 */
internal fun Dependency.stringCoordinates(): String = buildString {
    group?.let { append(it).append(':') }
    append(name)
    version?.let { append(':').append(it) }
}
