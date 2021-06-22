/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJavaToolchain.Companion.TOOLCHAIN_SUPPORTED_VERSION
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal interface ToolchainSupport {
    fun applyToolchain(action: Action<Any>)

    companion object {
        internal fun createToolchain(
            project: Project
        ): ToolchainSupport {
            val currentVersion = GradleVersion.version(project.gradle.gradleVersion)
            return when {
                currentVersion < TOOLCHAIN_SUPPORTED_VERSION -> project.objects.newInstance<NonExistingToolchainSupport>()
                else -> project.objects.newInstance<DefaultToolchainSupport>(
                    project.extensions,
                    project.tasks,
                    project.plugins
                )
            }
        }
    }
}

internal abstract class NonExistingToolchainSupport : ToolchainSupport {
    override fun applyToolchain(
        action: Action<Any>
    ) {
        throw GradleException("JavaToolchain support is only available from Gradle 6.7")
    }
}

internal abstract class DefaultToolchainSupport @Inject constructor(
    private val extensions: ExtensionContainer,
    private val tasks: TaskContainer,
    private val plugins: PluginContainer
) : ToolchainSupport {
    private val toolchainSpec: JavaToolchainSpec
        get() = extensions
            .getByType(JavaPluginExtension::class.java)
            .toolchain

    init {
        configureToolchain()
    }

    override fun applyToolchain(
        action: Action<Any>
    ) {
        action.execute(toolchainSpec)
        configureToolchain()
    }

    private fun configureToolchain() {
        plugins.withId("org.gradle.java-base") {
            tasks
                .withType<UsesKotlinJavaToolchain>()
                .configureEach {
                    // Only set when toolchain is configured
                    if (toolchainSpec.languageVersion.isPresent) {
                        val toolchainService = extensions.findByType(JavaToolchainService::class.java)!!
                        it.kotlinJavaToolchain.toolchain.use(
                            toolchainService.launcherFor(toolchainSpec)
                        )
                    }
                }
        }
    }
}
