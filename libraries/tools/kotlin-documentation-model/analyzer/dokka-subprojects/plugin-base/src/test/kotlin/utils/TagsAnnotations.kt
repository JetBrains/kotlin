/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import org.junit.jupiter.api.Tag


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

/**
 * Tests that pass with symbol-based Java analysis (AA) but fail with PSI-based Java analysis.
 * These tests verify AA-specific behavior that differs from PSI (e.g., Kotlin modality vs JVM bytecode modality).
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
@Tag("onlyJavaSymbols")
annotation class OnlyJavaSymbols(val reason: String = "")
