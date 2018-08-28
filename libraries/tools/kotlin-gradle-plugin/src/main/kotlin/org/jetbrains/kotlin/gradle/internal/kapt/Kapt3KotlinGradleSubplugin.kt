/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.builder.model.SourceProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.getKaptGeneratedClassesDir
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.getKaptGeneratedKotlinSourcesDir
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.getKaptGeneratedSourcesDir
import org.jetbrains.kotlin.gradle.internal.Kapt3KotlinGradleSubplugin.Companion.KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.model.builder.KaptModelBuilder
import org.jetbrains.kotlin.gradle.tasks.isWorkerAPISupported
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.util.*
import javax.inject.Inject

// apply plugin: 'kotlin-kapt'
class Kapt3GradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(Kapt3GradleSubplugin::class.java) != null

        @JvmStatic
        fun getKaptGeneratedClassesDir(project: Project, sourceSetName: String) =
                File(project.project.buildDir, "tmp/kapt3/classes/$sourceSetName")

        @JvmStatic
        fun getKaptGeneratedSourcesDir(project: Project, sourceSetName: String) =
                File(project.project.buildDir, "generated/source/kapt/$sourceSetName")

        @JvmStatic
        fun getKaptGeneratedKotlinSourcesDir(project: Project, sourceSetName: String) =
                File(project.project.buildDir, "generated/source/kaptKotlin/$sourceSetName")
    }

    override fun apply(project: Project) {
        project.extensions.create("kapt", KaptExtension::class.java)

        Kapt3KotlinGradleSubplugin().run {
            project.configurations.create(KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME).apply {
                project.getKotlinPluginVersion()?.let { kotlinPluginVersion ->
                    val kaptDependency = getPluginArtifact().run { "$groupId:$artifactId:$kotlinPluginVersion" }
                    dependencies.add(project.dependencies.create(kaptDependency))
                } ?: project.logger.error("Kotlin plugin should be enabled before 'kotlin-kapt'")
            }
        }
        registry.register(KaptModelBuilder())
    }
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

    open val annotationProcessorOptionProviders: List<*>
        get() = emptyList<Any>()
}

// Subplugin for the Kotlin Gradle plugin
class Kapt3KotlinGradleSubplugin : KotlinGradleSubplugin<KotlinCompile> {
    companion object {
        private val VERBOSE_OPTION_NAME = "kapt.verbose"
        private val USE_WORKER_API = "kapt.use.worker.api"
        private val INFO_AS_WARNINGS = "kapt.info.as.warnings"

        const val KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME = "kotlinKaptWorkerDependencies"

        private val KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated"

        val MAIN_KAPT_CONFIGURATION_NAME = "kapt"

        const val KAPT_ARTIFACT_NAME = "kotlin-annotation-processing-gradle"
        val KAPT_SUBPLUGIN_ID = "org.jetbrains.kotlin.kapt3"

        fun getKaptConfigurationName(sourceSetName: String): String {
            return if (sourceSetName != "main")
                "$MAIN_KAPT_CONFIGURATION_NAME${sourceSetName.capitalize()}"
            else
                MAIN_KAPT_CONFIGURATION_NAME
        }

        fun Project.findKaptConfiguration(sourceSetName: String): Configuration? {
            return project.configurations.findByName(getKaptConfigurationName(sourceSetName))
        }

        fun Project.isKaptVerbose(): Boolean {
            return hasProperty(VERBOSE_OPTION_NAME) && property(VERBOSE_OPTION_NAME) == "true"
        }

        fun Project.isUseWorkerApi(): Boolean {
            return isWorkerAPISupported() && hasProperty(USE_WORKER_API) && property(USE_WORKER_API) == "true"
        }

        fun Project.isInfoAsWarnings(): Boolean {
            return hasProperty(INFO_AS_WARNINGS) && property(INFO_AS_WARNINGS) == "true"
        }

        fun findMainKaptConfiguration(project: Project) = project.findKaptConfiguration(MAIN_KAPT_CONFIGURATION_NAME)

        fun createAptConfigurationIfNeeded(project: Project, sourceSetName: String): Configuration {
            val configurationName = Kapt3KotlinGradleSubplugin.getKaptConfigurationName(sourceSetName)

            project.configurations.findByName(configurationName)?.let { return it }
            val aptConfiguration = project.configurations.create(configurationName)

            if (aptConfiguration.name != Kapt3KotlinGradleSubplugin.MAIN_KAPT_CONFIGURATION_NAME) {
                // The main configuration can be created after the current one. We should handle this case
                val mainConfiguration = findMainKaptConfiguration(project)
                        ?: createAptConfigurationIfNeeded(project, SourceSet.MAIN_SOURCE_SET_NAME)

                aptConfiguration.extendsFrom(mainConfiguration)
            }

            return aptConfiguration
        }
    }

    private val kotlinToKaptGenerateStubsTasksMap = mutableMapOf<KotlinCompile, KaptGenerateStubsTask>()

    override fun isApplicable(project: Project, task: AbstractCompile) = task is KotlinCompile && Kapt3GradleSubplugin.isEnabled(project)

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
        val javaCompile: AbstractCompile?,
        val kaptVariantData: KaptVariantData<*>?,
        val sourceSetName: String,
        val kotlinCompilation: KotlinCompilation?,
        val kaptExtension: KaptExtension,
        val kaptClasspathConfigurations: List<Configuration>
    ) {
        val sourcesOutputDir = getKaptGeneratedSourcesDir(project, sourceSetName)
        val kotlinSourcesOutputDir = getKaptGeneratedKotlinSourcesDir(project, sourceSetName)
        val classesOutputDir = getKaptGeneratedClassesDir(project, sourceSetName)
    }

    override fun apply(
        project: Project,
        kotlinCompile: KotlinCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation?
    ): List<SubpluginOption> {
        assert((variantData != null) xor (kotlinCompilation != null))

        val buildDependencies = arrayListOf<TaskDependency>()
        val kaptConfigurations = arrayListOf<Configuration>()

        fun handleSourceSet(sourceSetName: String) {
            project.findKaptConfiguration(sourceSetName)?.let { kaptConfiguration ->
                kaptConfigurations += kaptConfiguration
                buildDependencies += kaptConfiguration.buildDependencies
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
            if (kotlinCompilation == null) error("In non-Android projects, Kotlin compilation should not be null")

            handleSourceSet(kotlinCompilation.compilationName)
            kotlinCompilation.compilationName
        }

        val kaptExtension = project.extensions.getByType(KaptExtension::class.java)

        val nonEmptyKaptConfigurations = kaptConfigurations.filter { it.dependencies.isNotEmpty() }

        val context = Kapt3SubpluginContext(
            project, kotlinCompile, javaCompile,
            kaptVariantData, sourceSetName, kotlinCompilation, kaptExtension, nonEmptyKaptConfigurations
        )

        val kaptGenerateStubsTask = context.createKaptGenerateStubsTask()
        val kaptTask = context.createKaptKotlinTask(useWorkerApi = project.isUseWorkerApi())

        kaptGenerateStubsTask.source(*kaptConfigurations.toTypedArray())

        kaptGenerateStubsTask.dependsOn(*buildDependencies.toTypedArray())
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

        val generatedFilesDir = getKaptGeneratedSourcesDir(project, sourceSetName)
        kaptVariantData?.addJavaSourceFoldersToModel(generatedFilesDir)

        pluginOptions += SubpluginOption("aptMode", aptMode)
        disableAnnotationProcessingInJavaTask()

        javaCompile?.source(generatedFilesDir)

        pluginOptions += FilesSubpluginOption("sources", listOf(generatedFilesDir))
        pluginOptions += FilesSubpluginOption("classes", listOf(getKaptGeneratedClassesDir(project, sourceSetName)))

        pluginOptions += FilesSubpluginOption("incrementalData", listOf(getKaptIncrementalDataDir()))

        val annotationProcessors = kaptExtension.processors
        if (annotationProcessors.isNotEmpty()) {
            pluginOptions += SubpluginOption("processors", annotationProcessors)
        }

        kotlinSourcesOutputDir.mkdirs()

        val apOptions = getAPOptions()

        pluginOptions += CompositeSubpluginOption("apoptions", encodeList(apOptions.associate { it.key to it.value }), apOptions)

        pluginOptions += SubpluginOption("javacArguments", encodeList(kaptExtension.getJavacOptions()))

        addMiscOptions(pluginOptions)

        return pluginOptions
    }

    private fun Kapt3SubpluginContext.getAPOptions(): List<SubpluginOption> {
        val androidPlugin = kaptVariantData?.let {
            project.extensions.findByName("android") as? BaseExtension
        }

        val androidOptions = kaptVariantData?.annotationProcessorOptions ?: emptyMap()

        val apOptionsFromProviders =
            if (isGradleVersionAtLeast(4, 6))
                kaptVariantData?.annotationProcessorOptionProviders
                    ?.flatMap { (it as CommandLineArgumentProvider).asArguments() }
                    .orEmpty()
            else
                emptyList()

        val subluginOptionsFromProvidedApOptions = apOptionsFromProviders.map {
            // Use the internal subplugin option type to exclude them from Gradle input/output checks, as their providers are already
            // properly registered as a nested input:

            // Pass options as they are in the key-only form (key = 'a=b'), kapt will deal with them:
            InternalSubpluginOption(key = it.removePrefix("-A"), value = "")
        }

        val apOptionsPairsList: List<Pair<String, String>> =
            kaptExtension.getAdditionalArguments(project, kaptVariantData?.variantData, androidPlugin).toList() +
                    androidOptions.toList()

        return apOptionsPairsList.map { SubpluginOption(it.first, it.second) } +
                FilesSubpluginOption(KAPT_KOTLIN_GENERATED, listOf(kotlinSourcesOutputDir)) +
                subluginOptionsFromProvidedApOptions
    }

    private fun Kapt3SubpluginContext.buildAndAddOptionsTo(task: Task, container: CompilerPluginOptions, aptMode: String) {
        val compilerPluginId = getCompilerPluginId()
        val kaptSubpluginOptions = buildOptions(aptMode)
        task.registerSubpluginOptionsAsInputs(compilerPluginId, kaptSubpluginOptions)

        for (option in kaptSubpluginOptions) {
            container.addPluginArgument(compilerPluginId, option)
        }

        // Also register all the subplugin options from the Kotlin task:
        project.afterEvaluate {
            kotlinCompile.pluginOptions.subpluginOptionsByPluginId.forEach { (pluginId, options) ->
                task.registerSubpluginOptionsAsInputs("kotlinCompile.$pluginId", options)
            }
        }
    }

    private fun encodeList(options: Map<String, String>): String {
        val os = ByteArrayOutputStream()
        val oos = ObjectOutputStream(os)

        oos.writeInt(options.size)
        for ((key, value) in options.entries) {
            oos.writeUTF(key)
            oos.writeUTF(value)
        }

        oos.flush()
        return Base64.getEncoder().encodeToString(os.toByteArray())
    }

    private fun Kapt3SubpluginContext.addMiscOptions(pluginOptions: MutableList<SubpluginOption>) {
        if (kaptExtension.generateStubs) {
            project.logger.warn("'kapt.generateStubs' is not used by the 'kotlin-kapt' plugin")
        }

        pluginOptions += SubpluginOption("useLightAnalysis", "${kaptExtension.useLightAnalysis}")
        pluginOptions += SubpluginOption("correctErrorTypes", "${kaptExtension.correctErrorTypes}")
        pluginOptions += SubpluginOption("mapDiagnosticLocations", "${kaptExtension.mapDiagnosticLocations}")
        pluginOptions += SubpluginOption("strictMode", "${kaptExtension.strictMode}")
        pluginOptions += SubpluginOption("infoAsWarnings", "${project.isInfoAsWarnings()}")
        pluginOptions += FilesSubpluginOption("stubs", listOf(getKaptStubsDir()))

        if (project.isKaptVerbose()) {
            pluginOptions += SubpluginOption("verbose", "true")
        }
    }

    private fun Kapt3SubpluginContext.createKaptKotlinTask(useWorkerApi: Boolean): KaptTask {
        val taskClass = if (useWorkerApi) KaptWithoutKotlincTask::class.java else KaptWithKotlincTask::class.java
        val kaptTask = project.tasks.create(getKaptTaskName("kapt"), taskClass)

        kaptTask.useBuildCache = kaptExtension.useBuildCache

        kaptTask.kotlinCompileTask = kotlinCompile

        kaptTask.stubsDir = getKaptStubsDir()
        kaptTask.destinationDir = sourcesOutputDir
        kaptTask.kotlinSourcesDestinationDir = kotlinSourcesOutputDir
        kaptTask.classesDir = classesOutputDir

        kotlinCompilation?.run {
            output.apply {
                if (tryAddClassesDir { project.files(classesOutputDir).builtBy(kaptTask) }) {
                    kotlinCompile.attachClassesDir { classesOutputDir }
                }
            }
        }

        kotlinCompile.source(sourcesOutputDir, kotlinSourcesOutputDir)

        if (javaCompile != null) {
            if (kaptVariantData != null) {
                kaptVariantData.registerGeneratedJavaSource(project, kaptTask, javaCompile)
            } else {
                registerGeneratedJavaSource(kaptTask, javaCompile)
            }
        }

        kaptTask.kaptClasspathConfigurations = kaptClasspathConfigurations

        kaptVariantData?.annotationProcessorOptionProviders?.let {
            kaptTask.annotationProcessorOptionProviders.add(it)
        }

        if (kaptTask is KaptWithKotlincTask) {
            buildAndAddOptionsTo(kaptTask, kaptTask.pluginOptions, aptMode = "apt")
        }

        if (kaptTask is KaptWithoutKotlincTask) {
            with(kaptTask) {
                isVerbose = project.isKaptVerbose()
                mapDiagnosticLocations = kaptExtension.mapDiagnosticLocations
                annotationProcessorFqNames = kaptExtension.processors.split(',').filter { it.isNotEmpty() }
                processorOptions = getAPOptions().map { it.key to it.value }.toMap()
                javacOptions = kaptExtension.getJavacOptions()
            }
        }

        return kaptTask
    }

    private fun Kapt3SubpluginContext.createKaptGenerateStubsTask(): KaptGenerateStubsTask {
        val kaptTask = project.tasks.create(
                getKaptTaskName("kaptGenerateStubs"),
                KaptGenerateStubsTask::class.java)

        kaptTask.sourceSetName = sourceSetName
        kaptTask.kotlinCompileTask = kotlinCompile
        kotlinToKaptGenerateStubsTasksMap[kotlinCompile] = kaptTask

        kaptTask.stubsDir = getKaptStubsDir()
        kaptTask.destinationDir = getKaptIncrementalDataDir()
        kaptTask.mapClasspath { kotlinCompile.classpath }
        kaptTask.generatedSourcesDir = sourcesOutputDir
        mapKotlinTaskProperties(project, kaptTask)

        kaptTask.kaptClasspathConfigurations = kaptClasspathConfigurations
        buildAndAddOptionsTo(kaptTask, kaptTask.pluginOptions, aptMode = "stubs")

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

            if (isGradleVersionAtLeast(4, 6)) {
                // Filter out the argument providers that are related to annotation processing and therefore already used by Kapt.
                // This is done to avoid outputs intersections between Kapt and and javaCompile and make the up-to-date check for
                // javaCompile more granular as it does not perform annotation processing:
                if (kaptVariantData != null) {
                    options.compilerArgumentProviders.removeAll(kaptVariantData.annotationProcessorOptionProviders)
                }
            }
        }
    }

    override fun getCompilerPluginId() = KAPT_SUBPLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = KAPT_ARTIFACT_NAME)
}

internal fun registerGeneratedJavaSource(kaptTask: KaptTask, javaTask: AbstractCompile) {
    javaTask.source(kaptTask.destinationDir)
}

internal fun Configuration.getNamedDependencies(): List<Dependency> = allDependencies.filter { it.group != null && it.name != null }

private val ANNOTATION_PROCESSOR = "annotationProcessor"
private val ANNOTATION_PROCESSOR_CAP = ANNOTATION_PROCESSOR.capitalize()

internal fun checkAndroidAnnotationProcessorDependencyUsage(project: Project) {
    if (project.hasProperty("kapt.dont.warn.annotationProcessor.dependencies")) {
        return
    }

    val isKapt3Enabled = Kapt3GradleSubplugin.isEnabled(project)

    val apConfigurations = project.configurations
        .filter { it.name == ANNOTATION_PROCESSOR || (it.name.endsWith(ANNOTATION_PROCESSOR_CAP) && !it.name.startsWith("_")) }

    val problemDependencies = mutableListOf<Dependency>()

    for (apConfiguration in apConfigurations) {
        val apConfigurationName = apConfiguration.name

        val kaptConfigurationName = when (apConfigurationName) {
            ANNOTATION_PROCESSOR -> "kapt"
            else -> {
                val configurationName = apConfigurationName.dropLast(ANNOTATION_PROCESSOR_CAP.length)
                Kapt3KotlinGradleSubplugin.getKaptConfigurationName(configurationName)
            }
        }

        val kaptConfiguration = project.configurations.findByName(kaptConfigurationName) ?: continue
        val kaptConfigurationDependencies = kaptConfiguration.getNamedDependencies()

        problemDependencies += apConfiguration.getNamedDependencies().filter { a ->
            // Ignore annotationProcessor dependencies if they are also declared as 'kapt'
            kaptConfigurationDependencies.none { k -> a.group == k.group && a.name == k.name && a.version == k.version }
        }
    }

    if (problemDependencies.isNotEmpty()) {
        val artifactsRendered = problemDependencies.joinToString { "'${it.group}:${it.name}:${it.version}'" }
        val andApplyKapt = if (isKapt3Enabled) "" else " and apply the kapt plugin: \"apply plugin: 'kotlin-kapt'\""

        project.logger.warn(
            "${project.name}: " +
                    "'annotationProcessor' dependencies won't be recognized as kapt annotation processors. " +
                    "Please change the configuration name to 'kapt' for these artifacts: $artifactsRendered$andApplyKapt."
        )
    }
}
