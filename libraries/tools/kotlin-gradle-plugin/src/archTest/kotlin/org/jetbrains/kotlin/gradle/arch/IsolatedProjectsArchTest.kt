/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.arch

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.freeze.FreezingArchRule.freeze
import org.jetbrains.kotlin.gradle.arch.config.kgpClasses
import kotlin.test.Test

class IsolatedProjectsArchTest {

    @Test
    fun noNewProjectRootProjectUsages() {
        freeze(
            noClasses()
                .should().callMethod("org.gradle.api.Project", "getRootProject")
                .because("Project.rootProject is incompatible with Isolated Projects.")
        ).check(kgpClasses)
    }
}
