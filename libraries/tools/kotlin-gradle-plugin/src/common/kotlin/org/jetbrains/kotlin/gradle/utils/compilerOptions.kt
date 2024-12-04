/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider

/**
 * All Kotlin plugins configure similar or the exact same properties.
 *
 * Since CompilerOptions have to be created in multiple places, common logic is hidden in Fabric extensions.
 */

internal fun ObjectFactory.KotlinCommonCompilerOptionsDefault(project: Project): KotlinCommonCompilerOptions {
    return newInstance<KotlinCommonCompilerOptionsDefault>()
        .configureCommonCompilerOptions(project)
}

internal fun ObjectFactory.KotlinJvmCompilerOptionsDefault(project: Project): KotlinJvmCompilerOptions {
    return newInstance<KotlinJvmCompilerOptionsDefault>()
        .configureCommonCompilerOptions(project)
}

internal fun ObjectFactory.KotlinJsCompilerOptionsDefault(project: Project): KotlinJsCompilerOptions {
    return newInstance<KotlinJsCompilerOptionsDefault>()
        .configureCommonCompilerOptions(project)
}

internal fun ObjectFactory.KotlinNativeCompilerOptionsDefault(project: Project): KotlinNativeCompilerOptions {
    return newInstance<KotlinNativeCompilerOptionsDefault>()
        .configureCommonCompilerOptions(project)
}

internal fun ObjectFactory.KotlinMultiplatformCommonCompilerOptionsDefault(project: Project): KotlinMultiplatformCommonCompilerOptions {
    return newInstance<KotlinMultiplatformCommonCompilerOptionsDefault>()
        .configureCommonCompilerOptions(project)
}

/**
 * Shared logic
 */

// could be private, but it's used by [org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlugin] right now
internal fun <T : KotlinCommonCompilerOptions> T.configureCommonCompilerOptions(
    project: Project
): T = apply {
    configureLogging(project.logger)
    configureExperimentalTryNext(project.kotlinPropertiesProvider, project.providers)
}

private fun <T : KotlinCommonCompilerOptions> T.configureLogging(
    logger: Logger
): T = apply {
    // Gradle log level is the convention for compiler log level.
    // "verbose" is historically used instead of a log level on [KotlinCommonCompilerToolOptions] level,
    // then some code in the compiler uses it to initialize the standard v/d/i/w/e log level internally.
    // If you're interested in the fine-grained compiler log level, please write about that use case
    // in https://youtrack.jetbrains.com/issue/KT-64698
    verbose.convention(logger.isDebugEnabled)
}

private fun <T : KotlinCommonCompilerOptions> T.configureExperimentalTryNext(
    kotlinProperties: PropertiesProvider,
    providerFactory: ProviderFactory
): T = apply {
    languageVersion.convention(
        kotlinProperties.kotlinExperimentalTryNext.mapOrNull(providerFactory) { enabled ->
            @Suppress("TYPE_MISMATCH")
            if (enabled) KotlinVersion.nextKotlinLanguageVersion else null
        }
    )
}

internal val KotlinVersion.Companion.nextKotlinLanguageVersion get() = KotlinVersion.values().first { it > KotlinVersion.DEFAULT }
