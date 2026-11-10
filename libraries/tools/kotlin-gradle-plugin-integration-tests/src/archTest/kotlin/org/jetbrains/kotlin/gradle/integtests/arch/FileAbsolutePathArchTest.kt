/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.integtests.arch

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.freeze.FreezingArchRule.freeze
import org.jetbrains.kotlin.gradle.integtests.arch.config.kgpIntegrationTestClasses
import kotlin.test.Test

class FileAbsolutePathArchTest {

    @Test
    fun noNewFileAbsolutePathUsages() {
        freeze(
            noClasses()
                .should().callMethod("java.io.File", "getAbsolutePath")
                .because(
                    "File.getAbsolutePath does not resolve symlinks, " +
                        "which creates problems for tests that often rely on JUnit's @TempDir, which on macOS refers to a symlinked directory"
                )
        ).check(kgpIntegrationTestClasses)
    }
}
