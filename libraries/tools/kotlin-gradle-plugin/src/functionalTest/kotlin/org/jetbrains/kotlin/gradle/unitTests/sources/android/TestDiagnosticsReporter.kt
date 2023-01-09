/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.sources.android

import org.jetbrains.kotlin.gradle.plugin.sources.android.checker.KotlinAndroidSourceSetLayoutChecker
import kotlin.test.fail

internal class TestDiagnosticsReporter : KotlinAndroidSourceSetLayoutChecker.DiagnosticReporter {

    class ErrorDiagnosticException(diagnostic: KotlinAndroidSourceSetLayoutChecker.Diagnostic) :
        KotlinAndroidSourceSetLayoutChecker.ProjectMisconfiguredException(diagnostic.message)

    private val _errors = mutableListOf<KotlinAndroidSourceSetLayoutChecker.Diagnostic>()

    private val _warnings = mutableListOf<KotlinAndroidSourceSetLayoutChecker.Diagnostic>()

    val errors get() = _errors.toList()

    val warnings get() = _warnings.toList()

    override fun error(diagnostic: KotlinAndroidSourceSetLayoutChecker.Diagnostic): Nothing {
        _errors.add(diagnostic)
        throw ErrorDiagnosticException(diagnostic)
    }

    override fun warning(diagnostic: KotlinAndroidSourceSetLayoutChecker.Diagnostic) {
        _warnings.add(diagnostic)
    }
}

internal fun TestDiagnosticsReporter.assertSingleWarning(): KotlinAndroidSourceSetLayoutChecker.Diagnostic {
    if (errors.isNotEmpty()) fail("Expected just a single warning, but found errors: $errors")
    if (warnings.isEmpty()) fail("Expected a single warning, but found none!")
    if (warnings.size > 1) fail("Expected a single warning, but found multiple: $warnings")
    return warnings.first()
}