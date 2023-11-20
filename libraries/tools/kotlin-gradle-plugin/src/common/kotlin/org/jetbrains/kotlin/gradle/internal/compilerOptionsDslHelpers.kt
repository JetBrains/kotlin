/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.utils.newInstance

internal fun KotlinMultiplatformExtension.syncCommonOptions(
    extensionCompilerOptions: KotlinCommonCompilerOptions
) {
    targets.configureEach { target ->
        KotlinCommonCompilerOptionsHelper.syncOptionsAsConvention(
            extensionCompilerOptions,
            target.targetCompilerOptions
        )
    }

    project.launch {
        awaitSourceSets().configureEach { kotlinSourceSet ->
            val defaultLanguageSettings = kotlinSourceSet.languageSettings as DefaultLanguageSettingsBuilder
            launch {
                KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
                if (!defaultLanguageSettings.compilationCompilerOptions.isCompleted) {
                    // For shared source sets without any associated compilation, it would be a separate instance of common compiler options
                    val compilerOptions = project.objects.newInstance<KotlinCommonCompilerOptionsDefault>()
                    KotlinCommonCompilerOptionsHelper.syncOptionsAsConvention(
                        extensionCompilerOptions,
                        compilerOptions
                    )
                    defaultLanguageSettings.compilationCompilerOptions.complete(compilerOptions)
                }
            }
        }
    }
}

internal val KotlinTarget.targetCompilerOptions: KotlinCommonCompilerOptions
    get() = when (this) {
        is HasConfigurableCompilerOptions<*> -> compilerOptions
        // Required for external targets that do not implement 'HasConfigurableCompilerOptions' interface
        is DecoratedExternalKotlinTarget -> delegate.compilerOptions
        else -> throw IllegalStateException("'KotlinTarget' type ${this.javaClass} does not allow to configure compiler options!")
    }
