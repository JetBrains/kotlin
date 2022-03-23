/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.configuration.KaptGenerateStubsConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KaptWithoutKotlincConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileConfig

/** Plugin that can be used by third-party plugins to create Kotlin-specific DSL and tasks (compilation and KAPT) for JVM platform. */
abstract class KotlinBaseApiPlugin : KotlinBasePlugin(), KotlinJvmFactory {

    private val logger = Logging.getLogger(KotlinBaseApiPlugin::class.java)
    override val pluginVersion = getKotlinPluginVersion(logger)

    private lateinit var myProject: Project
    private val taskCreator = KotlinTasksProvider()

    override fun apply(project: Project) {
        super.apply(project)
        myProject = project
        setupAttributeMatchingStrategy(project, isKotlinGranularMetadata = false)
    }

    override fun addCompilerPluginDependency(dependency: Any) {
        myProject.dependencies.add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, dependency)
    }

    override fun getCompilerPlugins(): FileCollection {
        return myProject.configurations.getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)
    }

    override fun createKotlinJvmDsl(): KotlinJvmOptions {
        return KotlinJvmOptionsImpl()
    }

    override val kotlinExtension: KotlinProjectExtension by lazy {
        myProject.objects.newInstance(KotlinProjectExtension::class.java, myProject)
    }

    override val kaptExtension: KaptExtension by lazy {
        myProject.objects.newInstance(KaptExtension::class.java)
    }

    override fun createKotlinCompileTask(taskName: String): TaskProvider<out KotlinJvmCompile> {
        return taskCreator.registerKotlinJVMTask(
            myProject, taskName, KotlinJvmOptionsImpl(), KotlinCompileConfig(myProject, kotlinExtension)
        )
    }

    override fun createKaptGenerateStubsTask(taskName: String): TaskProvider<out KaptGenerateStubsApi> {
        val taskConfig = KaptGenerateStubsConfig(myProject, kotlinExtension, kaptExtension)
        return myProject.registerTask(taskName, KaptGenerateStubsTask::class.java, emptyList()).also {
            taskConfig.execute(it)
        }
    }

    override fun createKaptTask(taskName: String): TaskProvider<out KaptTaskApi> {
        val taskConfiguration = KaptWithoutKotlincConfig(myProject, kaptExtension)
        return myProject.registerTask(taskName, KaptWithoutKotlincTask::class.java, emptyList()).also {
            taskConfiguration.execute(it)
        }
    }
}