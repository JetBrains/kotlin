/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider

internal fun <T : KotlinCommonCompilerOptions> T.configureExperimentalTryK2(
    project: Project,
    kotlinProperties: PropertiesProvider = project.kotlinPropertiesProvider
): T = configureExperimentalTryK2(kotlinProperties)

internal fun <T : KotlinCommonCompilerOptions> T.configureExperimentalTryK2(
    kotlinProperties: PropertiesProvider
): T = apply {
    languageVersion.convention(
        kotlinProperties.kotlinExperimentalTryK2.map { enabled ->
            @Suppress("TYPE_MISMATCH")
            if (enabled) KotlinVersion.KOTLIN_2_0 else null
        }
    )
}

internal fun Project.warnExperimentalTryK2IsEnabled() {
    if (project.kotlinPropertiesProvider.kotlinExperimentalTryK2.get()) {
        SingleWarningPerBuild.show(this, EXPERIMENTAL_TRY_K2_WARNING_MESSAGE)
    }
}

val EXPERIMENTAL_TRY_K2_WARNING_MESSAGE =
    """
    ATTENTION: 'kotlin.experimental.tryK2' is an experimental option enabled in the project for trying out the new Kotlin K2 compiler only.
    Please refrain from using it in production code and provide feedback to the Kotlin team for any issues encountered via https://kotl.in/issue
    """.trimIndent()