/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.problems.AdditionalData
import org.gradle.api.provider.Property

/**
 * Kotlin tooling diagnostic [additional data][AdditionalData] passed to Gradle Problem API spec.
 *
 * Only available in builds running on Gradle 9.6 and above.
 *
 * @since 2.4.20
 */
interface KotlinToolingDiagnosticsAdditionalData : AdditionalData {

    /**
     * The diagnostic tooling severity.
     *
     * Always configured.
     *
     * @since 2.4.20
     */
    val severity: Property<KotlinToolingDiagnosticsSeverity>
}
