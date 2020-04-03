/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.configureDefaultVersionsResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsSingleTargetPreset
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSingleTargetPreset
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility

open class KotlinJsPlugin(
    private val kotlinPluginVersion: String
) : Plugin<Project> {

    override fun apply(project: Project) {
        // TODO get rid of this plugin, too? Use the 'base' plugin instead?
        // in fact, the attributes schema of the Java base plugin may be required to consume non-MPP Kotlin/JS libs,
        // so investigation is needed
        project.plugins.apply(JavaBasePlugin::class.java)

        checkGradleCompatibility()

        val kotlinExtension = project.kotlinExtension as KotlinJsProjectExtension
        configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)

        kotlinExtension.apply {
            irPreset = KotlinJsIrSingleTargetPreset(project, kotlinPluginVersion)
            legacyPreset = KotlinJsSingleTargetPreset(project, kotlinPluginVersion)
            defaultJsCompilerType = PropertiesProvider(project).jsCompiler
        }

        project.whenEvaluated {
            if (kotlinExtension._target == null) {
                project.logger.warn(
                    """
                        Please initialize the Kotlin/JS target. Use:
                        kotlin {
                            js {
                                // To build distributions for and run tests on browser or Node.js use one or both of:
                                browser()
                                nodejs()
                            }
                        }
                    """.trimIndent()
                )
            }
        }

        // Explicitly create configurations for main and test
        // It is because in single platform we want to declare dependencies with methods not with strings in Kotlin DSL
        // implementation("foo") instead of "implementation"("foo")
        val configurations = project.configurations
        listOf(MAIN_COMPILATION_NAME, TEST_COMPILATION_NAME)
            // in main compilation we don't need additional name
            .map { it.removeSuffix(MAIN_COMPILATION_NAME) }
            .forEach { baseCompilationName ->
                listOf(
                    COMPILE_ONLY,
                    IMPLEMENTATION,
                    API,
                    RUNTIME_ONLY
                ).forEach { baseConfigurationName ->
                    configurations.maybeCreate(
                        lowerCamelCaseName(
                            baseCompilationName,
                            baseConfigurationName
                        )
                    )
                }
            }

        // Also create predefined source sets
        kotlinExtension.sourceSets.maybeCreate(MAIN_COMPILATION_NAME)
        kotlinExtension.sourceSets.maybeCreate(TEST_COMPILATION_NAME)
    }
}