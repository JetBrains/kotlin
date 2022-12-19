/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

internal class KotlinNativeCompileConfig : TaskConfigAction<KotlinNativeCompile> {
    constructor(compilationInfo: KotlinCompilationInfo) : super(compilationInfo.project) {
        configureTask { task ->
            task.incremental.convention(false)
            task.useModuleDetection.convention(false)
            task.friendPaths.from({ compilationInfo.friendPaths })
            compilationInfo.tcsOrNull?.compilation?.let { compilation ->
                task.pluginClasspath.from(
                    compilation.internal.configurations.pluginConfiguration
                )
            }
            task.moduleName.set(providers.provider { compilationInfo.moduleName })
            task.sourceSetName.value(providers.provider { compilationInfo.compilationName })
            task.multiPlatformEnabled.value(
                providers.provider {
                    compilationInfo.project.plugins.any {
                        it is KotlinPlatformPluginBase ||
                                it is AbstractKotlinMultiplatformPluginWrapper ||
                                it is AbstractKotlinPm20PluginWrapper
                    }
                }
            )

            task.compilerOptions.apply {
                val defaultOptions = compilationInfo.compilerOptions.options

                useK2.convention(defaultOptions.useK2)
                apiVersion.convention(defaultOptions.apiVersion)
                languageVersion.convention(defaultOptions.languageVersion)
                verbose.convention(defaultOptions.verbose)
                allWarningsAsErrors.convention(defaultOptions.allWarningsAsErrors)
                suppressWarnings.convention(defaultOptions.suppressWarnings)
            }
        }
    }
}
