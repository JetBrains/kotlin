/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider

internal fun <T : KotlinCommonCompilerOptions> T.configureExperimentalTryNext(
    project: Project,
    kotlinProperties: PropertiesProvider = project.kotlinPropertiesProvider
): T = configureExperimentalTryNext(kotlinProperties, project.providers)

internal val KotlinVersion.Companion.nextKotlinLanguageVersion get() = KotlinVersion.values().first { it > KotlinVersion.DEFAULT }

internal fun <T : KotlinCommonCompilerOptions> T.configureExperimentalTryNext(
    kotlinProperties: PropertiesProvider,
    providerFactory: ProviderFactory,
): T = apply {
    languageVersion.convention(
        kotlinProperties.kotlinExperimentalTryNext.mapOrNull(providerFactory) { enabled ->
            @Suppress("TYPE_MISMATCH")
            if (enabled) KotlinVersion.nextKotlinLanguageVersion else null
        }
    )
}
