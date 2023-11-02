/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.ReadyForExecution
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.registerEmbedAndSignAppleFrameworkTask

open class KotlinNativeTargetConfigurator<T : KotlinNativeTarget> : AbstractKotlinTargetConfigurator<T>(
    createTestCompilation = true
) {


    // FIXME support creating interop tasks for PM20

    // endregion.

    // region Configuration.
    override fun configurePlatformSpecificModel(target: T) {
        if (target.konanTarget.family.isAppleFamily) {
            registerEmbedAndSignAppleFrameworkTasks(target)
        }

        if (PropertiesProvider(target.project).ignoreIncorrectNativeDependencies != true) {
            warnAboutIncorrectDependencies(target)
        }
    }



    private fun registerEmbedAndSignAppleFrameworkTasks(target: KotlinNativeTarget) {
        val project = target.project
        target.binaries.withType(Framework::class.java).all { framework ->
            project.registerEmbedAndSignAppleFrameworkTask(framework)
        }
    }

    private fun warnAboutIncorrectDependencies(target: KotlinNativeTarget) = target.project.launchInStage(ReadyForExecution) {

        val compileOnlyDependencies = target.compilations.mapNotNull {
            val dependencies = project.configurations.getByName(it.compileOnlyConfigurationName).allDependencies
            if (dependencies.isNotEmpty()) {
                it to dependencies
            } else null
        }

        fun Dependency.stringCoordinates(): String = buildString {
            group?.let { append(it).append(':') }
            append(name)
            version?.let { append(':').append(it) }
        }

        if (compileOnlyDependencies.isNotEmpty()) {
            with(target.project.logger) {
                warn("A compileOnly dependency is used in the Kotlin/Native target '${target.name}':")
                compileOnlyDependencies.forEach {
                    warn(
                        """
                        Compilation: ${it.first.name}

                        Dependencies:
                        ${it.second.joinToString(separator = "\n") { it.stringCoordinates() }}

                        """.trimIndent()
                    )
                }
                warn(
                    """
                    Such dependencies are not applicable for Kotlin/Native, consider changing the dependency type to 'implementation' or 'api'.
                    To disable this warning, set the $KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES=true project property
                    """.trimIndent()
                )
            }
        }
    }
    // endregion.

    object NativeArtifactFormat {
        const val KLIB = "org.jetbrains.kotlin.klib"
        const val FRAMEWORK = "org.jetbrains.kotlin.framework"
    }

    companion object {
        const val INTEROP_GROUP = "interop"
        const val RUN_GROUP = "run"


    }
}

