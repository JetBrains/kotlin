/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.InvalidUserCodeException
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.jupiter.api.Assumptions
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JvmSourceSetCreatedBeforeCompilationTest {
    @Test
    fun `test - kmp - jvm source set created before compilation`() {
        Assumptions.assumeTrue(GradleVersion.current() >= GradleVersion.version("9.0"))
        val project = buildProjectWithMPP()
        project.multiplatformExtension.sourceSets.create("jvmCustom")
        assertFailsWith<InvalidUserCodeException> { project.multiplatformExtension.jvm().compilations.create("custom") }
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.JvmSourceSetCreatedBeforeCompilation)
    }
}
