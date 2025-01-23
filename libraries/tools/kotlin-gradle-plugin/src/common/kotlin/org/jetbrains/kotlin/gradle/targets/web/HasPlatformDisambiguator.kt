/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web

import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

/**
 * Represents entities that can provide platform-specific disambiguation for names of tasks and extensions.
 * Commonly used in scenarios where a platform's identifier needs to be prefixed
 * It is necessary to not duplicate calculation of platform-specific names
 */
internal interface HasPlatformDisambiguator {
    /**
     * This property is commonly used as a prefix or part of identifier names to distinguish configurations,
     * extensions, or tasks across multiple platforms.
     */
    val platformDisambiguator: String?

    /**
     * Generates an extension name by combining the platform-specific disambiguator with the provided base name
     *
     * @param baseName The base name to be combined with the platform-specific disambiguator.
     * @return A string representing the combined name
     */
    fun extensionName(baseName: String): String =
        lowerCamelCaseName(platformDisambiguator.orEmpty(), baseName)
}