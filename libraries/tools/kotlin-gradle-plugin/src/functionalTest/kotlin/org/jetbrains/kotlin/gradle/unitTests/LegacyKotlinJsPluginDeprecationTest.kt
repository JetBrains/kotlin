/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.util.assertThrows
import kotlin.test.Test
import kotlin.test.assertContains

class LegacyKotlinJsPluginDeprecationTest {
    @Test
    fun `legacy js plugin - emits error diagnostic during configuration`() {
        assertContains(
            assertThrows<Exception> {
                buildProject {
                    plugins.apply("org.jetbrains.kotlin.js")
                }.evaluate()
            }.stackTraceToString(),
            KotlinToolingDiagnostics.DeprecatedKotlinJsPlugin().message,
        )
    }
}
