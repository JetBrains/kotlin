/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config

/**
 * A policy to generate name mappings in sourcemaps.
 *
 * Sourcemaps can map names from the generated JavaScript code to the names of the corresponding entities in the Kotlin source code.
 */
enum class SourceMapNamesPolicy {
    /**
     * Don't generate name mappings
     */
    NO,

    /**
     * Generate name mappings. For functions and methods, map to their simple names instead of fully-qualified names.
     *
     * This is the default.
     */
    SIMPLE_NAMES,

    /**
     * Generate name mappings. For functions and methods, map to their fully-qualified names.
     */
    FULLY_QUALIFIED_NAMES,
}