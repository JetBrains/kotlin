/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.arch

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.jetbrains.kotlin.gradle.arch.config.kgpClasses
import kotlin.test.Test

/**
 * Checks that every implementation of a public Kotlin Gradle Plugin model type also extends its internal counterpart.
 *
 * The public types live in `kotlin-gradle-plugin-api` and can't expose plugin internals. Instead, the plugin downcasts
 * them through the `.internal` extension properties, such as `KotlinTarget.internal`, to reach state like extras or
 * observable `dependsOn` edges (see KT-52726 and KT-54981). An implementation that lacks the internal type compiles,
 * but those downcasts fail at runtime.
 */
class InternalImplementationsArchTest {

    @Test
    fun kotlinCompilationsImplementInternalKotlinCompilation() {
        assertAllImplementationsAlsoImplement(
            "org.jetbrains.kotlin.gradle.plugin.KotlinCompilation",
            "org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation",
        )
    }

    @Test
    fun kotlinTargetsImplementInternalKotlinTarget() {
        assertAllImplementationsAlsoImplement(
            "org.jetbrains.kotlin.gradle.plugin.KotlinTarget",
            "org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTarget",
        )
    }

    @Test
    fun kotlinTargetComponentsImplementInternalKotlinTargetComponent() {
        assertAllImplementationsAlsoImplement(
            "org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent",
            "org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTargetComponent",
        )
    }

    @Test
    fun kotlinSourceSetsImplementInternalKotlinSourceSet() {
        assertAllImplementationsAlsoImplement(
            "org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet",
            "org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet",
        )
    }

    private fun assertAllImplementationsAlsoImplement(baseInterface: String, requiredType: String) {
        classes()
            .that().areAssignableTo(baseInterface)
            .and().areNotInterfaces()
            .should().beAssignableTo(requiredType)
            .check(kgpClasses)
    }
}
