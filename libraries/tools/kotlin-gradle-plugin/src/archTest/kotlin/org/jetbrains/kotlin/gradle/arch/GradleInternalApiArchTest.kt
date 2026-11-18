/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.arch

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.freeze.FreezingArchRule.freeze
import org.jetbrains.kotlin.gradle.arch.config.kgpClasses
import kotlin.test.Test

class GradleInternalApiArchTest {

    @Test
    fun noNewGradleInternalApiUsages() {
        freeze(
            noClasses()
                .should().dependOnClassesThat().resideInAPackage("org.gradle..internal..")
                .because("Gradle internal API is not covered by Gradle compatibility guarantees and may break between Gradle versions.")
        ).check(kgpClasses)
    }
}
