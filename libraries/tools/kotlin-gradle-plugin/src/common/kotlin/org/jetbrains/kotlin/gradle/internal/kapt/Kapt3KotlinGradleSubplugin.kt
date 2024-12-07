/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.internal.kapt.KaptProperties
import org.jetbrains.kotlin.gradle.model.builder.KaptModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.configuration.KaptGenerateStubsConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KaptWithoutKotlincConfig
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.util.*
import java.util.concurrent.Callable
import javax.inject.Inject

// apply plugin: 'kotlin-kapt'
class Kapt3GradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("kapt", KaptExtension::class.java)

        registry.register(KaptModelBuilder())
    }

    companion object {
        // Required for IDEA import
        @Suppress("unused")
        @JvmStatic
        fun getKaptGeneratedClassesDir(project: Project, sourceSetName: String): File =
            project.getKaptGeneratedClassesDirectory(sourceSetName).get().asFile

        internal fun Project.getKaptGeneratedClassesDirectory(sourceSetName: String) =
            layout.buildDirectory.dir("tmp/kapt3/classes/$sourceSetName")

        // Required for IDEA import
        @Suppress("unused")
        @JvmStatic
        fun getKaptGeneratedSourcesDir(project: Project, sourceSetName: String): File =
            project.getKaptGeneratedSourcesDirectory(sourceSetName).get().asFile

        internal fun Project.getKaptGeneratedSourcesDirectory(sourceSetName: String) =
            layout.buildDirectory.dir("generated/source/kapt/$sourceSetName")

        // Required for IDEA import
        @Suppress("unused")
        @JvmStatic
        fun getKaptGeneratedKotlinSourcesDir(project: Project, sourceSetName: String) =
            project.getKaptGeneratedKotlinSourcesDirectory(sourceSetName).get().asFile

        internal fun Project.getKaptGeneratedKotlinSourcesDirectory(sourceSetName: String) =
            layout.buildDirectory.dir("generated/source/kaptKotlin/$sourceSetName")

        const val KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME = "kotlinKaptWorkerDependencies"

        val KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated"

        val MAIN_KAPT_CONFIGURATION_NAME = "kapt"

        const val KAPT_ARTIFACT_NAME = "kotlin-annotation-processing-gradle"
        val KAPT_SUBPLUGIN_ID = "org.jetbrains.kotlin.kapt3"

        fun getKaptConfigurationName(sourceSetName: String): String {
            return if (sourceSetName != SourceSet.MAIN_SOURCE_SET_NAME)
                "$MAIN_KAPT_CONFIGURATION_NAME${sourceSetName.capitalizeAsciiOnly()}"
            else
                MAIN_KAPT_CONFIGURATION_NAME
        }

        fun Project.findKaptConfiguration(sourceSetName: String): Configuration? {
            return project.configurations.findByName(getKaptConfigurationName(sourceSetName))
        }

        fun Project.disableClassloaderCacheForProcessors(): Set<String> {
            return KaptProperties.getClassloadersCacheDisableForProcessors(project).get()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }

        fun findMainKaptConfiguration(project: Project) = project.findKaptConfiguration(SourceSet.MAIN_SOURCE_SET_NAME)

        fun createAptConfigurationIfNeeded(project: Project, sourceSetName: String): Configuration {
            val configurationName = getKaptConfigurationName(sourceSetName)

            project.configurations.findResolvable(configurationName)?.let { return it }
            val aptConfiguration = project.configurations.createResolvable(configurationName).apply {
                attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
            }

            if (aptConfiguration.name != MAIN_KAPT_CONFIGURATION_NAME) {
                // The main configuration can be created after the current one. We should handle this case
                val mainConfiguration = findMainKaptConfiguration(project)
                    ?: createAptConfigurationIfNeeded(project, SourceSet.MAIN_SOURCE_SET_NAME)

                aptConfiguration.extendsFrom(mainConfiguration)
            }

            return aptConfiguration
        }

        fun isEnabled(project: Project) =
            project.plugins.any { it is Kapt3GradleSubplugin }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) =
        (kotlinCompilation.platformType == KotlinPlatformType.jvm || kotlinCompilation.platformType == KotlinPlatformType.androidJvm)

    private fun Kapt3SubpluginContext.getKaptStubsDir() = temporaryKaptDirectory("stubs")

    private fun Kapt3SubpluginContext.getKaptIncrementalDataDir() = temporaryKaptDirectory("incrementalData")

    private fun Kapt3SubpluginContext.getKaptIncrementalAnnotationProcessingCache() = temporaryKaptDirectory("incApCache")

    private fun Kapt3SubpluginContext.temporaryKaptDirectory(
        name: String
    ) = project.layout.buildDirectory.dir("tmp/kapt3/$name/$sourceSetName")

    internal inner class Kapt3SubpluginContext(
        val project: Project,
        val javaCompile: TaskProvider<out AbstractCompile>?,
        val variantData: Any?,
        val sourceSetName: String,
        val kotlinCompilation: KotlinCompilation<*>,
        val kaptExtension: KaptExtension,
        val kaptClasspathConfigurations: List<Configuration>
    ) {
        val sourcesOutputDir = project.getKaptGeneratedSourcesDirectory(sourceSetName)
        val kotlinSourcesOutputDir = project.getKaptGeneratedKotlinSourcesDirectory(sourceSetName)
        val classesOutputDir = project.getKaptGeneratedClassesDirectory(sourceSetName)

        @Suppress("UNCHECKED_CAST")
        val kotlinCompile: TaskProvider<KotlinCompile>
            // Can't use just kotlinCompilation.compileKotlinTaskProvider, as the latter is not statically-known to be KotlinCompile
            get() = kotlinCompilation.compileTaskProvider as TaskProvider<KotlinCompile>
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val buildDependencies = arrayListOf<TaskDependency>()
        val kaptConfigurations = arrayListOf<Configuration>()

        fun handleSourceSet(sourceSetName: String) {
            project.findKaptConfiguration(sourceSetName)?.let { kaptConfiguration ->
                kaptConfigurations += kaptConfiguration
                buildDependencies += kaptConfiguration.buildDependencies
            }
        }

        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
        val androidVariantData: DeprecatedAndroidBaseVariant? = (kotlinCompilation as? KotlinJvmAndroidCompilation)?.androidVariant

        val sourceSetName = if (androidVariantData != null) {
            for (provider in androidVariantData.sourceSets) {
                @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
                handleSourceSet((provider as DeprecatedAndroidSourceSet).name)
            }
            androidVariantData.name
        } else {
            handleSourceSet(kotlinCompilation.compilationName)
            kotlinCompilation.compilationName
        }

        val kaptExtension = project.extensions.getByType(KaptExtension::class.java)

        val javaCompileOrNull = findJavaTaskForKotlinCompilation(kotlinCompilation)

        val context = Kapt3SubpluginContext(
            project,
            javaCompileOrNull,
            androidVariantData,
            sourceSetName,
            kotlinCompilation,
            kaptExtension,
            kaptConfigurations,
        )

        val kaptGenerateStubsTaskProvider: TaskProvider<KaptGenerateStubsTask> = context.createKaptGenerateStubsTask()
        val kaptTaskProvider: TaskProvider<out KaptTask> = context.createKaptKotlinTask(
            kaptGenerateStubsTaskProvider
        )

        kaptGenerateStubsTaskProvider.configure { kaptGenerateStubsTask ->
            kaptGenerateStubsTask.dependsOn(*buildDependencies.toTypedArray())

            if (androidVariantData != null) {
                kaptGenerateStubsTask.additionalSources.from(
                    Callable {
                        // Avoid circular dependency: the stubs task need the Java sources, but the Java sources generated by Kapt should be
                        // excluded, as the Kapt tasks depend on the stubs ones, and having them in the input would lead to a cycle
                        val kaptJavaOutput = kaptTaskProvider.get().destinationDir.get().asFile
                        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
                        androidVariantData.getSourceFolders(DeprecatedAndroidSourceKind.JAVA).filter { it.dir != kaptJavaOutput }
                    }
                )
            }
        }

        context.kotlinCompile.configure { it.dependsOn(kaptTaskProvider) }

        /** Plugin options are applied to kapt*Compile inside [createKaptKotlinTask] */
        return project.provider { emptyList<SubpluginOption>() }
    }

    /* Returns AP options from static DSL. */
    private fun Kapt3SubpluginContext.getDslKaptApOptions(): Provider<List<SubpluginOption>> = project.provider {
        val androidVariantData = KaptWithAndroid.androidVariantData(this)

        val androidOptions = androidVariantData?.annotationProcessorOptions ?: emptyMap()
        val androidSubpluginOptions = androidOptions.toList().map { SubpluginOption(it.first, it.second) }

        androidSubpluginOptions + getNonAndroidDslApOptions(
            kaptExtension, project, listOf(kotlinSourcesOutputDir.get().asFile)
        ).get()
    }

    private fun Kapt3SubpluginContext.createKaptKotlinTask(
        generateStubsTask: TaskProvider<KaptGenerateStubsTask>
    ): TaskProvider<out KaptTask> {
        val taskName = kotlinCompile.kaptTaskName
        val taskConfigAction = KaptWithoutKotlincConfig(
            kotlinCompilation.project,
            generateStubsTask,
            kaptExtension
        )

        val kaptClasspathConfiguration = project.configurations.createResolvable("kaptClasspath_$taskName")
            .setExtendsFrom(kaptClasspathConfigurations).also {
                it.isVisible = false
            }
        taskConfigAction.configureTaskProvider { taskProvider ->
            taskProvider.dependsOn(generateStubsTask)

            if (javaCompile != null) {
                val androidVariantData = KaptWithAndroid.androidVariantData(this)
                if (androidVariantData != null) {
                    KaptWithAndroid.registerGeneratedJavaSourceForAndroid(this, project, androidVariantData, taskProvider)
                    androidVariantData.addJavaSourceFoldersToModel(sourcesOutputDir.get().asFile)
                } else {
                    registerGeneratedJavaSource(taskProvider, javaCompile)
                }

                disableAnnotationProcessingInJavaTask()
            }

            // Workaround for changes in Gradle 7.3 causing eager task realization
            // For details check `KotlinSourceSetProcessor.prepareKotlinCompileTask()`
            if (kotlinCompilation is KotlinWithJavaCompilation<*, *>) {
                kotlinCompilation.output.classesDirs.from(classesOutputDir).builtBy(taskProvider)
            } else {
                kotlinCompilation.output.classesDirs.from(taskProvider.flatMap { it.classesDir })
            }

            kotlinCompilation.compileTaskProvider.configure { task ->
                with(task as AbstractKotlinCompile<*>) {
                    setSource(sourcesOutputDir, kotlinSourcesOutputDir)
                    libraries.from(classesOutputDir)
                }
            }
        }
        taskConfigAction.configureTask { task ->
            task.stubsDir.set(getKaptStubsDir())
            task.destinationDir.set(sourcesOutputDir)
            task.kotlinSourcesDestinationDir.set(kotlinSourcesOutputDir)
            task.classesDir.set(classesOutputDir)

            if (javaCompile != null) {
                task.defaultJavaSourceCompatibility.set(javaCompile.map { it.sourceCompatibility })
            }

            task.incAptCache.value(
                KaptProperties.isIncrementalKapt(project).flatMap {
                    if (it) getKaptIncrementalAnnotationProcessingCache() else project.provider { null }
                }
            ).disallowChanges()

            task.kaptClasspath.from(kaptClasspathConfiguration).disallowChanges()
            task.kaptExternalClasspath.from(
                kaptClasspathConfiguration
                    .incoming
                    .artifactView { artifactView ->
                        artifactView.componentFilter {
                            it is ModuleComponentIdentifier
                        }
                    }
                    .files
            )
            task.kaptClasspathConfigurationNames.value(kaptClasspathConfigurations.map { it.name }).disallowChanges()

            KaptWithAndroid.androidVariantData(this)?.annotationProcessorOptionProviders?.let {
                task.annotationProcessorOptionProviders.add(it)
            }

            val pluginOptions: Provider<CompilerPluginOptions> = getDslKaptApOptions().toCompilerPluginOptions()

            task.kaptPluginOptions.add(pluginOptions)
        }

        return project.registerTask(taskName, KaptWithoutKotlincTask::class.java, emptyList()).also {
            taskConfigAction.execute(it)
        }
    }

    private fun Kapt3SubpluginContext.createKaptGenerateStubsTask(): TaskProvider<KaptGenerateStubsTask> {
        val kaptTaskName = kotlinCompile.kaptGenerateStubsTaskName
        val kaptTaskProvider = project.registerTask<KaptGenerateStubsTask>(kaptTaskName, listOf(project))

        val taskConfig = KaptGenerateStubsConfig(kotlinCompilation)
        taskConfig.configureTask {
            it.stubsDir.set(getKaptStubsDir())
            it.destinationDirectory.set(getKaptIncrementalDataDir())
            it.kaptClasspath.from(kaptClasspathConfigurations)
        }

        taskConfig.execute(kaptTaskProvider)

        project.whenEvaluated {
            addCompilationSourcesToExternalCompileTask(kotlinCompilation, kaptTaskProvider)
        }

        return kaptTaskProvider
    }

    private fun Kapt3SubpluginContext.disableAnnotationProcessingInJavaTask() {
        javaCompile?.configure { javaCompileInstance ->
            if (javaCompileInstance !is JavaCompile)
                return@configure

            val options = javaCompileInstance.options
            // 'android-apt' (com.neenbedankt) adds a File instance to compilerArgs (List<String>).
            // Although it's not our problem, we need to handle this case properly.
            val oldCompilerArgs: List<Any> = options.compilerArgs
            val newCompilerArgs = oldCompilerArgs.filterTo(mutableListOf()) {
                it !is CharSequence || !it.toString().startsWith("-proc:")
            }
            if (!kaptExtension.keepJavacAnnotationProcessors) {
                newCompilerArgs.add("-proc:none")
            }
            @Suppress("UNCHECKED_CAST")
            options.compilerArgs = newCompilerArgs as List<String>

            // Filter out the argument providers that are related to annotation processing and therefore already used by Kapt.
            // This is done to avoid outputs intersections between Kapt and and javaCompile and make the up-to-date check for
            // javaCompile more granular as it does not perform annotation processing:
            KaptWithAndroid.androidVariantData(this)?.let { androidVariantData ->
                options.compilerArgumentProviders.removeAll(androidVariantData.annotationProcessorOptionProviders)
            }
        }
    }

    override fun getCompilerPluginId() = KAPT_SUBPLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = KAPT_ARTIFACT_NAME)
}

internal const val KAPT_GENERATE_STUBS_PREFIX = "kaptGenerateStubs"
internal const val KAPT_PREFIX = "kapt"

internal val TaskProvider<out KotlinJvmCompile>.kaptGenerateStubsTaskName
    get() = getKaptTaskName(name, KAPT_GENERATE_STUBS_PREFIX)

internal val TaskProvider<out KotlinJvmCompile>.kaptTaskName
    get() = getKaptTaskName(name, KAPT_PREFIX)

internal fun getKaptTaskName(
    kotlinCompileName: String,
    prefix: String
): String {
    return if (kotlinCompileName.startsWith("compile")) {
        // Replace compile*Kotlin to kapt*Kotlin
        kotlinCompileName.replaceFirst("compile", prefix)
    } else {
        // Task was created via exposed apis (KotlinJvmFactory or MPP) with random name
        // in such case adding 'kapt' prefix to name
        "$prefix${kotlinCompileName.capitalizeAsciiOnly()}"
    }
}

internal fun buildKaptSubpluginOptions(
    kaptExtension: KaptExtension,
    project: Project,
    javacOptions: Map<String, String>,
    aptMode: String,
    generatedSourcesDir: Iterable<File>,
    generatedClassesDir: Iterable<File>,
    incrementalDataDir: Iterable<File>,
    includeCompileClasspath: Boolean,
    kaptStubsDir: Iterable<File>,
): List<SubpluginOption> {
    if (kaptExtension.generateStubs) {
        project.logger.warn("'kapt.generateStubs' is not used by the 'kotlin-kapt' plugin")
    }

    val pluginOptions = mutableListOf<SubpluginOption>()

    pluginOptions += SubpluginOption("aptMode", aptMode)

    pluginOptions += FilesSubpluginOption("sources", generatedSourcesDir)
    pluginOptions += FilesSubpluginOption("classes", generatedClassesDir)
    pluginOptions += FilesSubpluginOption("incrementalData", incrementalDataDir)

    @Suppress("DEPRECATION") val annotationProcessors = kaptExtension.processors
    if (annotationProcessors.isNotEmpty()) {
        pluginOptions += SubpluginOption("processors", annotationProcessors)
    }
    pluginOptions += SubpluginOption("javacArguments", encodeList(javacOptions))
    pluginOptions += SubpluginOption("includeCompileClasspath", includeCompileClasspath.toString())

    // These option names must match those defined in org.jetbrains.kotlin.kapt.cli.KaptCliOption.
    pluginOptions += SubpluginOption("useLightAnalysis", "${kaptExtension.useLightAnalysis}")
    pluginOptions += SubpluginOption("correctErrorTypes", "${kaptExtension.correctErrorTypes}")
    pluginOptions += SubpluginOption("dumpDefaultParameterValues", "${kaptExtension.dumpDefaultParameterValues}")
    pluginOptions += SubpluginOption("mapDiagnosticLocations", "${kaptExtension.mapDiagnosticLocations}")
    pluginOptions += SubpluginOption(
        "strictMode", // Currently doesn't match KaptCliOption.STRICT_MODE_OPTION, is it a typo introduced in https://github.com/JetBrains/kotlin/commit/c83581e6b8155c6d89da977be6e3cd4af30562e5?
        "${kaptExtension.strictMode}"
    )
    pluginOptions += SubpluginOption("stripMetadata", "${kaptExtension.stripMetadata}")
    pluginOptions += SubpluginOption("keepKdocCommentsInStubs", "${KaptProperties.isKaptKeepKdocCommentsInStubs(project).get()}")
    pluginOptions += SubpluginOption("showProcessorTimings", "${kaptExtension.showProcessorStats}")
    pluginOptions += SubpluginOption("detectMemoryLeaks", kaptExtension.detectMemoryLeaks)
    pluginOptions += SubpluginOption("useK2", "${KaptProperties.isUseK2(project).get()}")
    pluginOptions += SubpluginOption("infoAsWarnings", "${KaptProperties.isInfoAsWarnings(project).get()}")
    pluginOptions += FilesSubpluginOption("stubs", kaptStubsDir)

    if (KaptProperties.isKaptVerbose(project).get()) {
        pluginOptions += SubpluginOption("verbose", "true")
    }

    return pluginOptions
}

/* Returns AP options from KAPT static DSL. */
internal fun getNonAndroidDslApOptions(
    kaptExtension: KaptExtension,
    project: Project,
    kotlinSourcesOutputDir: Iterable<File>,
): Provider<List<SubpluginOption>> {
    return project.provider {
        kaptExtension.getAdditionalArguments().toList()
            .map { SubpluginOption(it.first, it.second) } +
                FilesSubpluginOption(Kapt3GradleSubplugin.KAPT_KOTLIN_GENERATED, kotlinSourcesOutputDir)
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

// Don't reference the BaseVariant type in the Kapt plugin signatures, as those type references will fail to link when there's no Android
// Gradle plugin on the project's plugin classpath
private object KaptWithAndroid {
    // Avoid loading the BaseVariant type at call sites and instead lazily load it when evaluation reaches it in the body using inline:
    @Suppress("NOTHING_TO_INLINE", "TYPEALIAS_EXPANSION_DEPRECATION")
    inline fun androidVariantData(
        context: Kapt3GradleSubplugin.Kapt3SubpluginContext
    ): DeprecatedAndroidBaseVariant? = context.variantData as? DeprecatedAndroidBaseVariant

    @Suppress("NOTHING_TO_INLINE")
    // Avoid loading the BaseVariant type at call sites and instead lazily load it when evaluation reaches it in the body using inline:
    inline fun registerGeneratedJavaSourceForAndroid(
        kapt3SubpluginContext: Kapt3GradleSubplugin.Kapt3SubpluginContext,
        project: Project,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") variantData: DeprecatedAndroidBaseVariant,
        kaptTask: TaskProvider<out KaptTask>
    ) {
        val kaptSourceOutput = project.fileTree(kapt3SubpluginContext.sourcesOutputDir).builtBy(kaptTask)
        kaptSourceOutput.include("**/*.java")
        variantData.registerExternalAptJavaOutput(kaptSourceOutput)
        kaptTask.configure { kaptTaskInstance ->
            variantData.dataBindingDependencyArtifactsIfSupported?.let { dataBindingArtifacts ->
                kaptTaskInstance.dependsOn(dataBindingArtifacts)
            }
        }
    }
}

internal fun registerGeneratedJavaSource(kaptTask: TaskProvider<out KaptTask>, javaTaskProvider: TaskProvider<out AbstractCompile>) {
    javaTaskProvider.configure { javaTask ->
        val generatedJavaSources = javaTask.project.fileTree(kaptTask.flatMap { it.destinationDir })
        generatedJavaSources.include("**/*.java")
        javaTask.source(generatedJavaSources)
    }
}

internal fun Configuration.getNamedDependencies(): List<Dependency> = allDependencies.filter { it.group != null }

private val ANNOTATION_PROCESSOR = "annotationProcessor"
private val ANNOTATION_PROCESSOR_CAP = ANNOTATION_PROCESSOR.capitalizeAsciiOnly()

internal fun checkAndroidAnnotationProcessorDependencyUsage(project: Project) {
    project.whenKaptEnabled {
        if (KaptProperties.isKaptDontWarnAnnotationProcessorDependencies(project).get()) {
            return@whenKaptEnabled
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
                    Kapt3GradleSubplugin.getKaptConfigurationName(configurationName)
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
}

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
private val DeprecatedAndroidBaseVariant.annotationProcessorOptions: Map<String, String>?
    get() = javaCompileOptions.annotationProcessorOptions.arguments

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
private val DeprecatedAndroidBaseVariant.annotationProcessorOptionProviders: List<CommandLineArgumentProvider>
    get() = javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders

//TODO once the Android plugin reaches its 3.0.0 release, consider compiling against it (remove the reflective call)
@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
private val DeprecatedAndroidBaseVariant.dataBindingDependencyArtifactsIfSupported: FileCollection?
    get() = this::class.java.methods
        .find { it.name == "getDataBindingDependencyArtifacts" }
        ?.also { it.isAccessible = true }
        ?.invoke(this) as? FileCollection
