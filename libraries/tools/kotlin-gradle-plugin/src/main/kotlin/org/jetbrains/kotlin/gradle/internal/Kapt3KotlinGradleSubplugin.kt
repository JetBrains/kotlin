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
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import javax.xml.bind.DatatypeConverter.printBase64Binary

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

        fun getKaptClasssesDir(project: Project, sourceSetName: String): File =
                File(project.project.buildDir, "tmp/kapt3/classes/$sourceSetName")
    }

    private val kotlinToKaptGenerateStubsTasksMap = mutableMapOf<KotlinCompile, KaptGenerateStubsTask>()

    override fun isApplicable(project: Project, task: KotlinCompile) = Kapt3GradleSubplugin.isEnabled(project)

    fun getKaptGeneratedDir(project: Project, sourceSetName: String): File {
        return File(project.project.buildDir, "generated/source/kapt/$sourceSetName")
    }

    fun getKaptGeneratedDirForKotlin(project: Project, sourceSetName: String): File {
        return File(project.project.buildDir, "generated/source/kaptKotlin/$sourceSetName")
    }

    private fun Kapt3SubpluginContext.getKaptStubsDir() = createAndReturnTemporaryKaptDirectory("stubs")

    private fun Kapt3SubpluginContext.getKaptIncrementalDataDir() = createAndReturnTemporaryKaptDirectory("incrementalData")

    private fun Kapt3SubpluginContext.createAndReturnTemporaryKaptDirectory(name: String): File {
        val dir = File(project.buildDir, "tmp/kapt3/$name/$sourceSetName")
        dir.mkdirs()
        return dir
    }

    private inner class Kapt3SubpluginContext(
            val project: Project,
            val kotlinCompile: KotlinCompile,
            val javaCompile: AbstractCompile,
            val variantData: Any?,
            val sourceSetName: String,
            val kaptExtension: KaptExtension,
            val kaptClasspath: MutableList<File>) {
        val sourcesOutputDir = getKaptGeneratedDir(project, sourceSetName)
        val kotlinSourcesOutputDir = getKaptGeneratedDirForKotlin(project, sourceSetName)
        val classesOutputDir = getKaptClasssesDir(project, sourceSetName)

        val kaptClasspathArtifacts = project
                .resolveSubpluginArtifacts(listOf(this@Kapt3KotlinGradleSubplugin))
                .flatMap { it.value }
    }

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
            val filteredDependencies = kaptConfiguration?.dependencies?.filter {
                it.group != getGroupName() || it.name != getArtifactName()
            } ?: emptyList()

            if (kaptConfiguration != null && filteredDependencies.isNotEmpty()) {
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

        val kaptGenerateStubsTask = context.createKaptGenerateStubsTask()
        context.createKaptKotlinTask(kaptGenerateStubsTask)

        /** Plugin options are applied to kapt*Compile inside [createKaptKotlinTask] */
        return emptyList()
    }

    override fun getSubpluginKotlinTasks(project: Project, kotlinCompile: KotlinCompile): List<AbstractCompile> {
        val kaptGenerateStubsTask = kotlinToKaptGenerateStubsTasksMap[kotlinCompile]
        return if (kaptGenerateStubsTask == null) emptyList() else listOf(kaptGenerateStubsTask)
    }

    // This method should be called no more than once for each Kapt3SubpluginContext
    private fun Kapt3SubpluginContext.buildOptions(aptMode: String): List<SubpluginOption> {
        val pluginOptions = mutableListOf<SubpluginOption>()

        val generatedFilesDir = getKaptGeneratedDir(project, sourceSetName)
        if (variantData != null) {
            (variantData as BaseVariantData<*>).addJavaSourceFoldersToModel(generatedFilesDir)
        }

        pluginOptions += SubpluginOption("aptMode", aptMode)
        disableAnnotationProcessingInJavaTask()

        // Skip annotation processing in kotlinc if no kapt dependencies were provided
        if (kaptClasspath.isEmpty()) return pluginOptions

        kaptClasspath.forEach { pluginOptions += SubpluginOption("apclasspath", it.absolutePath) }

        javaCompile.source(generatedFilesDir)

        pluginOptions += SubpluginOption("sources", generatedFilesDir.canonicalPath)
        pluginOptions += SubpluginOption("classes", getKaptClasssesDir(project, sourceSetName).canonicalPath)

        pluginOptions += SubpluginOption("incrementalData", getKaptIncrementalDataDir().canonicalPath)

        val annotationProcessors = kaptExtension.processors
        if (annotationProcessors.isNotEmpty()) {
            pluginOptions += SubpluginOption("processors", annotationProcessors)
        }

        val androidPlugin = variantData?.let {
            project.extensions.findByName("android") as? BaseExtension
        }

        val androidOptions = if (variantData != null)
            AndroidGradleWrapper.getAnnotationProcessorOptionsFromAndroidVariant(variantData) ?: emptyMap()
        else
            emptyMap()

        kotlinSourcesOutputDir.mkdirs()

        val apOptions = kaptExtension.getAdditionalArguments(project, variantData, androidPlugin) +
                androidOptions +
                mapOf("kapt.kotlin.generated" to kotlinSourcesOutputDir.absolutePath)

        pluginOptions += SubpluginOption("apoptions", encodeOptions(apOptions))

        pluginOptions += SubpluginOption("javacArguments", encodeOptions(kaptExtension.getJavacOptions()))

        addMiscOptions(pluginOptions)

        return pluginOptions
    }

    private fun Kapt3SubpluginContext.buildAndAddOptionsTo(container: CompilerPluginOptions, aptMode: String) {
        val compilerPluginId = getCompilerPluginId()
        for (option in buildOptions(aptMode)) {
            container.addPluginArgument(compilerPluginId, option.key, option.value)
        }
    }

    fun encodeOptions(options: Map<String, String>): String {
        val os = ByteArrayOutputStream()
        val oos = ObjectOutputStream(os)

        oos.writeInt(options.size)
        for ((k, v) in options.entries) {
            oos.writeUTF(k)
            oos.writeUTF(v)
        }

        oos.flush()
        return printBase64Binary(os.toByteArray())
    }

    private fun Kapt3SubpluginContext.addMiscOptions(pluginOptions: MutableList<SubpluginOption>) {
        if (kaptExtension.generateStubs) {
            project.logger.warn("'kapt.generateStubs' is not used by the 'kotlin-kapt' plugin")
        }

        pluginOptions += SubpluginOption("useLightAnalysis", "${kaptExtension.useLightAnalysis}")
        pluginOptions += SubpluginOption("correctErrorTypes", "${kaptExtension.correctErrorTypes}")
        pluginOptions += SubpluginOption("stubs", getKaptStubsDir().canonicalPath)

        if (project.hasProperty(VERBOSE_OPTION_NAME) && project.property(VERBOSE_OPTION_NAME) == "true") {
            pluginOptions += SubpluginOption("verbose", "true")
        }
    }

    private fun Kapt3SubpluginContext.createKaptKotlinTask(kaptGenerateStubsTask: KaptGenerateStubsTask) {
        val kaptTask = project.tasks.create(getKaptTaskName("kapt"), KaptTask::class.java)
        kaptTask.kotlinCompileTask = kotlinCompile

        kaptClasspathArtifacts.forEach { kaptTask.pluginOptions.addClasspathEntry(it) }

        kaptTask.stubsDir = getKaptStubsDir()
        kaptTask.destinationDir = sourcesOutputDir
        kaptTask.mapClasspath { kotlinCompile.classpath }
        kaptTask.classesDir = classesOutputDir

        kaptTask.mapSource {
            val sourcesFromKotlinTask = kotlinCompile.source
                    .filter { it.extension == "java" && !kaptTask.isInsideDestinationDirs(it) }
                    .asFileTree

            val stubSources = project.fileTree(kaptTask.stubsDir)

            sourcesFromKotlinTask + stubSources
        }

        kaptTask.dependsOn(kaptGenerateStubsTask)
        kotlinCompile.dependsOn(kaptTask)

        // Add generated source dir as a source root for kotlinCompile and javaCompile
        kotlinCompile.source(sourcesOutputDir, kotlinSourcesOutputDir)
        javaCompile.source(sourcesOutputDir)

        buildAndAddOptionsTo(kaptTask.pluginOptions, aptMode = "apt")
    }

    private fun Kapt3SubpluginContext.createKaptGenerateStubsTask(): KaptGenerateStubsTask {
        val kaptTask = project.tasks.create(getKaptTaskName("kaptGenerateStubs"), KaptGenerateStubsTask::class.java)
        kaptTask.kotlinCompileTask = kotlinCompile
        kotlinToKaptGenerateStubsTasksMap[kotlinCompile] = kaptTask

        kaptClasspathArtifacts.forEach { kaptTask.pluginOptions.addClasspathEntry(it) }

        kaptTask.stubsDir = getKaptStubsDir()
        kaptTask.destinationDir = getKaptIncrementalDataDir()
        kaptTask.mapClasspath { kotlinCompile.classpath }
        kaptTask.generatedSourcesDir = sourcesOutputDir

        kaptTask.dependsOn(*(javaCompile.dependsOn.filter { it !== kotlinCompile }.toTypedArray()))

        buildAndAddOptionsTo(kaptTask.pluginOptions, aptMode = "stubs")

        return kaptTask
    }

    private fun Kapt3SubpluginContext.getKaptTaskName(prefix: String): String {
        // Replace compile*Kotlin to kapt*Kotlin
        val baseName = kotlinCompile.name
        assert(baseName.startsWith("compile"))
        return baseName.replaceFirst("compile", prefix)
    }

    private fun Kapt3SubpluginContext.disableAnnotationProcessingInJavaTask() {
        (javaCompile as? JavaCompile)?.let { javaCompile ->
            val options = javaCompile.options
            // 'android-apt' (com.neenbedankt) adds a File instance to compilerArgs (List<String>).
            // Although it's not our problem, we need to handle this case properly.
            val oldCompilerArgs: List<Any> = options.compilerArgs
            val newCompilerArgs = oldCompilerArgs.filterTo(mutableListOf()) {
                it !is CharSequence || !it.toString().startsWith("-proc:")
            }
            newCompilerArgs.add("-proc:none")
            @Suppress("UNCHECKED_CAST")
            options.compilerArgs = newCompilerArgs as List<String>
        }
    }

    private val BaseVariantData<*>.sourceProviders: List<SourceProvider>
        get() = variantConfiguration.sortedSourceProviders

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.kapt3"
    override fun getGroupName() = "org.jetbrains.kotlin"
    override fun getArtifactName() = "kotlin-annotation-processing"
}