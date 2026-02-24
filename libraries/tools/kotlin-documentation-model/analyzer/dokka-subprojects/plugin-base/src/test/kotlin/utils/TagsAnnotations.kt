/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import org.junit.jupiter.api.Tag


/**
 * Run a test only for descriptors, not symbols.
 *
 * In theory, these tests can be fixed
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlyDescriptors")
annotation class OnlyDescriptors(val reason: String = "")


/**
 * Run a test only for symbols (aka K2), not descriptors (K1).
 *
 * After remove K1 in dokka, this annotation should be also removed without consequences
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlySymbols")
annotation class OnlySymbols(val reason: String = "")

/**
 * Run a test only for descriptors, not symbols.
 *
 * These tests cannot be fixed until Analysis API does not support MPP
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlyDescriptorsMPP")
annotation class OnlyDescriptorsMPP(val reason: String = "")

/**
 * Run a test only for a new KDoc resolution (with K2)
 *
 * After we switch to the new KDoc resolution by default, this annotation should be removed
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlyNewKDocResolution")
annotation class OnlyNewKDocResolution(val reason: String = "")

/**
 * Tests that pass with PSI-based Java analysis but fail with symbol-based Java analysis.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlyJavaPsi")
annotation class OnlyJavaPsi(val reason: String = "")
