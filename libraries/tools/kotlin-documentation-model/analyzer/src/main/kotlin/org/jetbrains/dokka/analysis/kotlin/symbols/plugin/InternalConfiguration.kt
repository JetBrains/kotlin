/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

// revisit in scope of https://github.com/Kotlin/dokka/issues/2776
internal object InternalConfiguration {
    private const val ALLOW_KOTLIN_PACKAGE_PROPERTY = "org.jetbrains.dokka.analysis.allowKotlinPackage"

    /**
     * Allow analysing code in the 'kotlin' package
     *
     * Default: false
     *
     * @see org.jetbrains.kotlin.config.AnalysisFlags.allowKotlinPackage
     */
    val allowKotlinPackage: Boolean
        get() = getBooleanProperty(ALLOW_KOTLIN_PACKAGE_PROPERTY)

    private fun getBooleanProperty(propertyName: String): Boolean {
        return System.getProperty(propertyName) in setOf("1", "true")
    }
}