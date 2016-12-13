/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.model.SourceProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

// apply plugin: 'kotlin-kapt'
class Kapt3GradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(Kapt3GradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {}
}

// Subplugin for the Kotlin Gradle plugin
class Kapt3KotlinGradleSubplugin : KotlinGradleSubplugin<KotlinCompile> {
    companion object {
        private val VERBOSE_OPTION_NAME = "kapt.verbose"

        val MAIN_KAPT_CONFIGURATION_NAME = "kapt"

        fun getKaptConfigurationName(sourceSetName: String): String {
            return if (sourceSetName != "main")
                "$MAIN_KAPT_CONFIGURATION_NAME${sourceSetName.capitalize()}"
            else
                MAIN_KAPT_CONFIGURATION_NAME
        }

        fun Project.findKaptConfiguration(sourceSetName: String): Configuration? {
            return project.configurations.findByName(getKaptConfigurationName(sourceSetName))
        }

        fun findMainKaptConfiguration(project: Project) = project.findKaptConfiguration(MAIN_KAPT_CONFIGURATION_NAME)
    }

    private val kotlinToKaptTasksMap = mutableMapOf<KotlinCompile, KaptTask>()

    override fun isApplicable(project: Project, task: KotlinCompile) = Kapt3GradleSubplugin.isEnabled(project)

    fun getKaptGeneratedDir(project: Project, sourceSetName: String): File {
        return File(project.project.buildDir, "generated/source/kapt/$sourceSetName")
    }

    fun getKaptStubsDir(project: Project, sourceSetName: String): File {
        val dir = File(project.project.buildDir, "tmp/kapt3/stubs/$sourceSetName")
        dir.mkdirs()
        return dir
    }

    private class Kapt3SubpluginContext(
            val project: Project,
            val kotlinCompile: KotlinCompile,
            val javaCompile: AbstractCompile,
            val variantData: Any?,
            val sourceSetName: String,
            val kaptExtension: KaptExtension,
            val kaptClasspath: MutableList<File>)

    override fun apply(
            project: Project, 
            kotlinCompile: KotlinCompile,
            javaCompile: AbstractCompile,
            variantData: Any?,
            javaSourceSet: SourceSet?
    ): List<SubpluginOption> {
        assert((variantData != null) xor (javaSourceSet != null))

        val kaptClasspath = arrayListOf<File>()

        fun handleSourceSet(sourceSetName: String) {
            val kaptConfiguration = project.findKaptConfiguration(sourceSetName)
            if (kaptConfiguration != null && kaptConfiguration.dependencies.size > 1) {
                javaCompile.dependsOn(kaptConfiguration.buildDependencies)
                kaptClasspath.addAll(kaptConfiguration.resolve())
            }
        }

        val sourceSetName = if (variantData != null) {
            for (provider in (variantData as BaseVariantData<*>).sourceProviders) {
                handleSourceSet((provider as AndroidSourceSet).name)
            }

            variantData.name
        }
        else {
            if (javaSourceSet == null) error("Java source set should not be null")

            handleSourceSet(javaSourceSet.name)
            javaSourceSet.name
        }

        val kaptExtension = project.extensions.getByType(KaptExtension::class.java)

        val context = Kapt3SubpluginContext(project, kotlinCompile, javaCompile,
                variantData, sourceSetName, kaptExtension, kaptClasspath)

        context.createKaptKotlinTask()

        /** Plugin options are applied to kapt*Compile inside [createKaptKotlinTask] */
        return emptyList()
    }

    override fun getSubpluginKotlinTasks(project: Project, kotlinCompile: KotlinCompile): List<KaptTask> {
        return kotlinToKaptTasksMap[kotlinCompile]?.let { listOf(it) } ?: emptyList()
    }

    // This method should be called no more than once for each Kapt3SubpluginContext
    private fun Kapt3SubpluginContext.buildOptions(): List<SubpluginOption> {
        val pluginOptions = mutableListOf<SubpluginOption>()

        val generatedFilesDir = getKaptGeneratedDir(project, sourceSetName)
        if (variantData != null) {
            (variantData as BaseVariantData<*>).addJavaSourceFoldersToModel(generatedFilesDir)
        }

        pluginOptions += SubpluginOption("aptOnly", "true")
        disableAnnotationProcessingInJavaTask()

        // Skip annotation processing in kotlinc if no kapt dependencies were provided
        if (kaptClasspath.isEmpty()) return pluginOptions

        kaptClasspath.forEach { pluginOptions += SubpluginOption("apclasspath", it.absolutePath) }

        javaCompile.source(generatedFilesDir)

        pluginOptions += SubpluginOption("sources", generatedFilesDir.canonicalPath)
        pluginOptions += SubpluginOption("classes", kotlinCompile.destinationDir.canonicalPath)

        val androidPlugin = variantData?.let {
            project.extensions.findByName("android") as? BaseExtension
        }

        val androidOptions = if (variantData != null)
            AndroidGradleWrapper.getAnnotationProcessorOptionsFromAndroidVariant(variantData) ?: emptyMap()
        else
            emptyMap()

        val apOptions = kaptExtension.getAdditionalArguments(project, variantData, androidPlugin) + androidOptions

        for ((key, value) in apOptions) {
            pluginOptions += SubpluginOption("apoption", "$key:$value")
        }

        addMiscOptions(pluginOptions)

        return pluginOptions
    }

    private fun Kapt3SubpluginContext.addMiscOptions(pluginOptions: MutableList<SubpluginOption>) {
        if (kaptExtension.generateStubs) {
            project.logger.warn("'kapt.generateStubs' is not used by the 'kotlin-kapt' plugin")
        }

        pluginOptions += SubpluginOption("useLightAnalysis", "${kaptExtension.useLightAnalysis}")

        if (project.hasProperty(VERBOSE_OPTION_NAME) && project.property(VERBOSE_OPTION_NAME) == "true") {
            pluginOptions += SubpluginOption("verbose", "true")
            pluginOptions += SubpluginOption("stubs", getKaptStubsDir(project, sourceSetName).canonicalPath)
        }
    }

    private fun Kapt3SubpluginContext.createKaptKotlinTask() {
        val sourcesOutputDir = getKaptGeneratedDir(project, sourceSetName)

        // Replace compile*Kotlin to kapt*Kotlin
        assert(kotlinCompile.name.startsWith("compile"))
        val kaptTaskName = kotlinCompile.name.replaceFirst("compile", "kapt")
        val kaptTask = project.tasks.create(kaptTaskName, KaptTask::class.java)
        kaptTask.kotlinCompileTask = kotlinCompile
        kotlinToKaptTasksMap[kotlinCompile] = kaptTask

        project.resolveSubpluginArtifacts(listOf(this@Kapt3KotlinGradleSubplugin)).flatMap { it.value }.forEach {
            kaptTask.pluginOptions.addClasspathEntry(it)
        }

        kaptTask.mapClasspath { kotlinCompile.classpath }
        kaptTask.destinationDir = sourcesOutputDir
        kotlinCompile.dependsOn(kaptTask)

        // Add generated source dir as a source root for kotlinCompile and javaCompile
        kotlinCompile.source(sourcesOutputDir)
        javaCompile.source(sourcesOutputDir)

        val pluginOptions = kaptTask.pluginOptions
        val compilerPluginId = getCompilerPluginId()
        for (option in buildOptions()) {
            pluginOptions.addPluginArgument(compilerPluginId, option.key, option.value)
        }
    }

    private fun Kapt3SubpluginContext.disableAnnotationProcessingInJavaTask() {
        (javaCompile as? JavaCompile)?.let { javaCompile ->
            val options = javaCompile.options
            options.compilerArgs = options.compilerArgs.filter { !it.startsWith("-proc:") } + "-proc:none"
        }
    }

    private val BaseVariantData<*>.sourceProviders: List<SourceProvider>
        get() = variantConfiguration.sortedSourceProviders

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.kapt3"
    override fun getGroupName() = "org.jetbrains.kotlin"
    override fun getArtifactName() = "kotlin-annotation-processing"
}