/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import org.jetbrains.dokka.base.DokkaBaseInternalConfiguration
import org.jetbrains.dokka.base.DokkaBaseInternalConfiguration.SHOULD_DISPLAY_ALL_TYPES_PAGE_SYS_PROP
import org.jetbrains.dokka.base.DokkaBaseInternalConfiguration.SHOULD_DISPLAY_SINCE_KOTLIN_SYS_PROP

internal fun withAllTypesPage(block: () -> Unit): Unit =
    DokkaBaseInternalConfiguration.withProperty(SHOULD_DISPLAY_ALL_TYPES_PAGE_SYS_PROP, "true", block)

internal fun withSinceKotlin(block: () -> Unit): Unit =
    DokkaBaseInternalConfiguration.withProperty(SHOULD_DISPLAY_SINCE_KOTLIN_SYS_PROP, "true", block)

/**
 * This property works only for K2
 * Allow analysing code in the 'kotlin' package
 */
internal fun withAllowKotlinPackage(block: () -> Unit): Unit =
    DokkaBaseInternalConfiguration.withProperty("org.jetbrains.dokka.analysis.allowKotlinPackage", "true", block)

/**
 * This property works only for K2
 * Enable experimental KDoc resolution
 */
internal fun withExperimentalKDocResolution(block: () -> Unit): Unit =
    DokkaBaseInternalConfiguration.withProperty("org.jetbrains.dokka.analysis.enableExperimentalKDocResolution", "true", block)

internal fun DokkaBaseInternalConfiguration.withProperty(propertyName: String, value: String, block: () -> Unit) {
    setProperty(propertyName, value)
    try {
        block()
    } finally {
        clearProperty(propertyName)
    }
}

internal fun DokkaBaseInternalConfiguration.setProperty(propertyName: String, value: String) {
    System.setProperty(propertyName, value)
    reinitialize()
}

internal fun DokkaBaseInternalConfiguration.clearProperty(propertyName: String) {
    System.clearProperty(propertyName)
    reinitialize()
}
