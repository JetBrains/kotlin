/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.launch

internal fun Project.launchKotlinGradleProjectCheckers() {
    val checkers = kotlinGradleProjectCheckersOverride ?: KotlinGradleProjectChecker.ALL_CHECKERS

    val context = KotlinGradleProjectCheckerContext(
        project,
        project.kotlinPropertiesProvider,
        project.multiplatformExtensionOrNull
    )
    val collector = project.kotlinToolingDiagnosticsCollector

    for (checker in checkers) {
        with(checker) { launch { context.runChecks(collector) } }
    }
}

internal val Project.kotlinGradleProjectCheckersOverride: Collection<KotlinGradleProjectChecker>?
    get() {
        return if (extraProperties.has(KOTLIN_GRADLE_PROJECT_CHECKERS_OVERRIDE))
            @Suppress("unchecked_cast")
            extraProperties.get(KOTLIN_GRADLE_PROJECT_CHECKERS_OVERRIDE) as Collection<KotlinGradleProjectChecker>?
        else
            null
    }


internal const val KOTLIN_GRADLE_PROJECT_CHECKERS_OVERRIDE = "kotlin.internal.override.checkers"
