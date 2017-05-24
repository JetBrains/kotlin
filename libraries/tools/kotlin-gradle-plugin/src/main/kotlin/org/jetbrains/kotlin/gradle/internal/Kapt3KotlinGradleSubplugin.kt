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
import com.android.builder.model.SourceProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.plugin.*
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

abstract class KaptVariantData<T>(val variantData: T) {
    abstract val name: String
    abstract val sourceProviders: Iterable<SourceProvider>
    abstract fun addJavaSourceFoldersToModel(generatedFilesDir: File)
    abstract val annotationProcessorOptions: Map<String, String>?
    abstract fun registerGeneratedJavaSource(
            project: Project,
            kaptTask: KaptTask,
            javaTask: AbstractCompile)
}

// Subplugin for the Kotlin Gradle plugin
class Kapt3KotlinGradleSubplugin : KotlinGradleSubplugin<KotlinCompile> {
    companion object {
        private val VERBOSE_OPTION_NAME = "kapt.verbose"

        val MAIN_KAPT_CONFIGURATION_NAME = "kapt"

        val KAPT_GROUP_NAME = "org.jetbrains.kotlin"
        val KAPT_ARTIFACT_NAME = "kotlin-annotation-processing"

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
            val kaptVariantData: KaptVariantData<*>?,
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

        val kaptVariantData = variantData as? KaptVariantData<*>

        val sourceSetName = if (kaptVariantData != null) {
            for (provider in kaptVariantData.sourceProviders) {
                handleSourceSet((provider as AndroidSourceSet).name)
            }

            kaptVariantData.name
        }
        else {
            if (javaSourceSet == null) error("Java source set should not be null")

            handleSourceSet(javaSourceSet.name)
            javaSourceSet.name
        }

        val kaptExtension = project.extensions.getByType(KaptExtension::class.java)

        val context = Kapt3SubpluginContext(project, kotlinCompile, javaCompile,
                kaptVariantData, sourceSetName, kaptExtension, kaptClasspath)

        val kaptGenerateStubsTask = context.createKaptGenerateStubsTask()
        val kaptTask = context.createKaptKotlinTask()

        kaptGenerateStubsTask.dependsOn(*kotlinCompile.dependsOn.toTypedArray())
        kaptTask.dependsOn(kaptGenerateStubsTask)
        kotlinCompile.dependsOn(kaptTask)

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
        kaptVariantData?.addJavaSourceFoldersToModel(generatedFilesDir)

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

        val androidPlugin = kaptVariantData?.let {
            project.extensions.findByName("android") as? BaseExtension
        }

        val androidOptions = kaptVariantData?.annotationProcessorOptions ?: emptyMap()

        kotlinSourcesOutputDir.mkdirs()

        val apOptions = kaptExtension.getAdditionalArguments(
                project,
                kaptVariantData?.variantData,
                androidPlugin
        ) + androidOptions + mapOf("kapt.kotlin.generated" to kotlinSourcesOutputDir.absolutePath)

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

    private fun Kapt3SubpluginContext.createKaptKotlinTask(): KaptTask {
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

        kotlinCompile.source(sourcesOutputDir, kotlinSourcesOutputDir)

        if (kaptVariantData != null) {
            kaptVariantData.registerGeneratedJavaSource(project, kaptTask, javaCompile)
        }
        else {
            registerGeneratedJavaSource(kaptTask, javaCompile)
        }

        buildAndAddOptionsTo(kaptTask.pluginOptions, aptMode = "apt")
        return kaptTask
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
        mapKotlinTaskProperties(project, kaptTask)

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

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.kapt3"
    override fun getGroupName() = KAPT_GROUP_NAME
    override fun getArtifactName() = KAPT_ARTIFACT_NAME
}

internal fun registerGeneratedJavaSource(kaptTask: KaptTask, javaTask: AbstractCompile) {
    javaTask.source(kaptTask.destinationDir)
}

internal fun Configuration.getNamedDependencies(): List<Dependency> = allDependencies.filter { it.group != null && it.name != null }

internal fun checkAndroidAnnotationProcessorDependencyUsage(project: Project) {
    val isKapt3Enabled = Kapt3GradleSubplugin.isEnabled(project)

    val androidAPDependencies = project.configurations
            .filter { it.name == "annotationProcessor"
                    || (it.name.endsWith("AnnotationProcessor") && !it.name.startsWith("_")) }
            .flatMap { it.getNamedDependencies() }

    if (androidAPDependencies.isNotEmpty()) {
        val artifactsRendered = androidAPDependencies.joinToString { "'${it.group}:${it.name}:${it.version}'" }
        val andApplyKapt = if (isKapt3Enabled) "" else " and apply the kapt plugin: \"apply plugin: 'kotlin-kapt'\""
        project.logger.warn("${project.name}: " +
                "'androidProcessor' dependencies won't be recognized as kapt annotation processors. " +
                "Please change the configuration name to 'kapt' for these artifacts: $artifactsRendered$andApplyKapt.")
    }
}