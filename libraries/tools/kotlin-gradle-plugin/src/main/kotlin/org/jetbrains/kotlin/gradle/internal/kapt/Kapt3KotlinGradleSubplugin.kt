/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.lang.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.CLASS_STRUCTURE_ARTIFACT_TYPE
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformAction
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformLegacyAction
import org.jetbrains.kotlin.gradle.model.builder.KaptModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable
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

        target.configurations.create(KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME).apply {
            val kaptDependency = getPluginArtifact().run { "$groupId:$artifactId:${target.getKotlinPluginVersion()}" }
            dependencies.add(target.dependencies.create(kaptDependency))
            dependencies.add(
                target.kotlinDependency(
                    "kotlin-stdlib",
                    target.kotlinExtension.coreLibrariesVersion
                )
            )
        }

        registry.register(KaptModelBuilder())
    }

    companion object {
        @JvmStatic
        fun getKaptGeneratedClassesDir(project: Project, sourceSetName: String) =
            File(project.buildDir, "tmp/kapt3/classes/$sourceSetName")

        @JvmStatic
        fun getKaptGeneratedSourcesDir(project: Project, sourceSetName: String) =
            File(project.buildDir, "generated/source/kapt/$sourceSetName")

        @JvmStatic
        fun getKaptGeneratedKotlinSourcesDir(project: Project, sourceSetName: String) =
            File(project.buildDir, "generated/source/kaptKotlin/$sourceSetName")

        const val KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME = "kotlinKaptWorkerDependencies"

        private val KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated"

        private val CLASSLOADERS_CACHE_SIZE = "kapt.classloaders.cache.size"
        private val CLASSLOADERS_CACHE_DISABLE_FOR_PROCESSORS = "kapt.classloaders.cache.disableForProcessors"

        val MAIN_KAPT_CONFIGURATION_NAME = "kapt"

        const val KAPT_ARTIFACT_NAME = "kotlin-annotation-processing-gradle"
        val KAPT_SUBPLUGIN_ID = "org.jetbrains.kotlin.kapt3"

        fun getKaptConfigurationName(sourceSetName: String): String {
            return if (sourceSetName != SourceSet.MAIN_SOURCE_SET_NAME)
                "$MAIN_KAPT_CONFIGURATION_NAME${sourceSetName.capitalize()}"
            else
                MAIN_KAPT_CONFIGURATION_NAME
        }

        fun Project.findKaptConfiguration(sourceSetName: String): Configuration? {
            return project.configurations.findByName(getKaptConfigurationName(sourceSetName))
        }

        fun Project.isKaptVerbose(): Boolean {
            return getBooleanOptionValue(BooleanOption.KAPT_VERBOSE)
        }

        fun Project.isUseWorkerApi(): Boolean {
            return getBooleanOptionValue(BooleanOption.KAPT_USE_WORKER_API) {
                """
                |'${BooleanOption.KAPT_USE_WORKER_API}' is deprecated and scheduled to be removed in Kotlin 1.7 release.
                """.trimMargin()
            }
        }

        fun Project.isIncrementalKapt(): Boolean {
            return getBooleanOptionValue(BooleanOption.KAPT_INCREMENTAL_APT)
        }

        fun Project.isInfoAsWarnings(): Boolean {
            return getBooleanOptionValue(BooleanOption.KAPT_INFO_AS_WARNINGS)
        }

        fun Project.isIncludeCompileClasspath(): Boolean {
            return getBooleanOptionValue(BooleanOption.KAPT_INCLUDE_COMPILE_CLASSPATH)
        }

        fun Project.isKaptKeepKdocCommentsInStubs(): Boolean {
            return getBooleanOptionValue(BooleanOption.KAPT_KEEP_KDOC_COMMENTS_IN_STUBS)
        }

        fun Project.classLoadersCacheSize(): Int = findPropertySafe(CLASSLOADERS_CACHE_SIZE)?.toString()?.toInt() ?: 0

        fun Project.disableClassloaderCacheForProcessors(): Set<String> {
            val value = findPropertySafe(CLASSLOADERS_CACHE_DISABLE_FOR_PROCESSORS)?.toString() ?: ""
            return value
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }

        /**
         * In case [Project.findProperty] can throw exception, this version catch it and return null
         */
        private fun Project.findPropertySafe(propertyName: String): Any? =
            try {
                findProperty(propertyName)
            } catch (ex: Exception) {
                logger.warn("Error getting property $propertyName", ex)
                null
            }

        fun findMainKaptConfiguration(project: Project) = project.findKaptConfiguration(SourceSet.MAIN_SOURCE_SET_NAME)

        fun createAptConfigurationIfNeeded(project: Project, sourceSetName: String): Configuration {
            val configurationName = getKaptConfigurationName(sourceSetName)

            project.configurations.findByName(configurationName)?.let { return it }
            val aptConfiguration = project.configurations.create(configurationName).apply {
                // Should not be available for consumption from other projects during variant-aware dependency resolution:
                isCanBeConsumed = false
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
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

        private fun Project.getBooleanOptionValue(
            booleanOption: BooleanOption,
            deprecationMessage: (() -> String)? = null
        ): Boolean {
            val value = findProperty(booleanOption.optionName)
            if (value != null && deprecationMessage != null) {
                logger.warn(deprecationMessage())
            }
            return when (value) {
                is Boolean -> value
                is String -> when {
                    value.equals("true", ignoreCase = true) -> true
                    value.equals("false", ignoreCase = true) -> false
                    else -> {
                        project.logger.warn(
                            "Boolean option `${booleanOption.optionName}` was set to an invalid value: `$value`." +
                                    " Using default value `${booleanOption.defaultValue}` instead."
                        )
                        booleanOption.defaultValue
                    }
                }
                null -> booleanOption.defaultValue
                else -> {
                    project.logger.warn(
                        "Boolean option `${booleanOption.optionName}` was set to an invalid value: `$value`." +
                                " Using default value `${booleanOption.defaultValue}` instead."
                    )
                    booleanOption.defaultValue
                }
            }
        }

        /**
         * Kapt option that expects a Boolean value. It has a default value to be used when its value is not set.
         *
         * IMPORTANT: The default value should typically match those defined in org.jetbrains.kotlin.base.kapt3.KaptFlag.
         */
        private enum class BooleanOption(
            val optionName: String,
            val defaultValue: Boolean
        ) {
            KAPT_VERBOSE("kapt.verbose", false),
            KAPT_USE_WORKER_API(
                "kapt.use.worker.api", // Currently doesn't have a matching KaptFlag
                true
            ),
            KAPT_INCREMENTAL_APT(
                "kapt.incremental.apt",
                true // Currently doesn't match the default value of KaptFlag.INCREMENTAL_APT, but it's fine (see https://github.com/JetBrains/kotlin/pull/3942#discussion_r532578690).
            ),
            KAPT_INFO_AS_WARNINGS("kapt.info.as.warnings", false),
            KAPT_INCLUDE_COMPILE_CLASSPATH("kapt.include.compile.classpath", true),
            KAPT_KEEP_KDOC_COMMENTS_IN_STUBS("kapt.keep.kdoc.comments.in.stubs", true)
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) =
        (kotlinCompilation.platformType == KotlinPlatformType.jvm || kotlinCompilation.platformType == KotlinPlatformType.androidJvm)

    private fun Kapt3SubpluginContext.getKaptStubsDir() = temporaryKaptDirectory("stubs")

    private fun Kapt3SubpluginContext.getKaptIncrementalDataDir() = temporaryKaptDirectory("incrementalData")

    private fun Kapt3SubpluginContext.getKaptClasspathSnapshotDir() = temporaryKaptDirectory("classpath-snapshot")

    private fun Kapt3SubpluginContext.getKaptIncrementalAnnotationProcessingCache() = temporaryKaptDirectory("incApCache")

    private fun Kapt3SubpluginContext.temporaryKaptDirectory(
        name: String
    ) = project.buildDir.resolve("tmp/kapt3/$name/$sourceSetName")

    internal inner class Kapt3SubpluginContext(
        val project: Project,
        val javaCompile: TaskProvider<out AbstractCompile>?,
        val variantData: Any?,
        val sourceSetName: String,
        val kotlinCompilation: AbstractKotlinCompilation<*>,
        val kaptExtension: KaptExtension,
        val kaptClasspathConfigurations: List<Configuration>
    ) {
        val sourcesOutputDir = getKaptGeneratedSourcesDir(project, sourceSetName)
        val kotlinSourcesOutputDir = getKaptGeneratedKotlinSourcesDir(project, sourceSetName)
        val classesOutputDir = getKaptGeneratedClassesDir(project, sourceSetName)
        val includeCompileClasspath =
            kaptExtension.includeCompileClasspath
                ?: project.isIncludeCompileClasspath()

        val kotlinCompile: TaskProvider<KotlinCompile>
            // Can't use just kotlinCompilation.compileKotlinTaskProvider, as the latter is not statically-known to be KotlinCompile
            get() = checkNotNull(project.locateTask(kotlinCompilation.compileKotlinTaskName))
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

        val androidVariantData: BaseVariant? = (kotlinCompilation as? KotlinJvmAndroidCompilation)?.androidVariant

        val sourceSetName = if (androidVariantData != null) {
            for (provider in androidVariantData.sourceSets) {
                handleSourceSet((provider as AndroidSourceSet).name)
            }
            androidVariantData.name
        } else {
            handleSourceSet(kotlinCompilation.compilationName)
            kotlinCompilation.compilationName
        }

        val kaptExtension = project.extensions.getByType(KaptExtension::class.java)

        val nonEmptyKaptConfigurations = kaptConfigurations.filter { it.dependencies.isNotEmpty() }

        val javaCompileOrNull = findJavaTaskForKotlinCompilation(kotlinCompilation)

        val context = Kapt3SubpluginContext(
            project, javaCompileOrNull,
            androidVariantData, sourceSetName, kotlinCompilation as AbstractKotlinCompilation<*>/*TODO?*/, kaptExtension, nonEmptyKaptConfigurations
        )

        val kaptGenerateStubsTaskProvider: TaskProvider<KaptGenerateStubsTask> = context.createKaptGenerateStubsTask()
        val kaptTaskProvider: TaskProvider<out KaptTask> = context.createKaptKotlinTask(useWorkerApi = project.isUseWorkerApi())

        kaptGenerateStubsTaskProvider.configure { kaptGenerateStubsTask ->
            kaptGenerateStubsTask.dependsOn(*buildDependencies.toTypedArray())
            kaptGenerateStubsTask.dependsOn(
                project.provider {
                    kotlinCompilation.compileKotlinTask.dependsOn.filter { it !is TaskProvider<*> || it.name != kaptTaskProvider.name }
                }
            )

            if (androidVariantData != null) {
                kaptGenerateStubsTask.inputs.files(
                    Callable {
                        // Avoid circular dependency: the stubs task need the Java sources, but the Java sources generated by Kapt should be
                        // excluded, as the Kapt tasks depend on the stubs ones, and having them in the input would lead to a cycle
                        val kaptJavaOutput = kaptTaskProvider.get().destinationDir
                        androidVariantData.getSourceFolders(SourceKind.JAVA).filter { it.dir != kaptJavaOutput }
                    }
                ).withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }

        kaptTaskProvider.configure { kaptTask ->
            kaptTask.dependsOn(kaptGenerateStubsTaskProvider)
        }
        context.kotlinCompile.configure { it.dependsOn(kaptTaskProvider) }

        /** Plugin options are applied to kapt*Compile inside [createKaptKotlinTask] */
        return project.provider { emptyList<SubpluginOption>() }
    }

    // This method should be called no more than once for each Kapt3SubpluginContext
    private fun Kapt3SubpluginContext.buildOptions(
        aptMode: String,
        javacOptions: Provider<Map<String, String>>
    ): Provider<List<SubpluginOption>> {
        disableAnnotationProcessingInJavaTask()

        return project.provider {
            val pluginOptions = mutableListOf<SubpluginOption>()

            val generatedFilesDir = getKaptGeneratedSourcesDir(project, sourceSetName)
            KaptWithAndroid.androidVariantData(this)?.addJavaSourceFoldersToModel(generatedFilesDir)

            pluginOptions += SubpluginOption("aptMode", aptMode)

            pluginOptions += FilesSubpluginOption("sources", listOf(generatedFilesDir))
            pluginOptions += FilesSubpluginOption("classes", listOf(getKaptGeneratedClassesDir(project, sourceSetName)))

            pluginOptions += FilesSubpluginOption("incrementalData", listOf(getKaptIncrementalDataDir()))

            val annotationProcessors = kaptExtension.processors
            if (annotationProcessors.isNotEmpty()) {
                pluginOptions += SubpluginOption("processors", annotationProcessors)
            }

            if (aptMode == "apt") {
                // apOptions are needed only for "apt" mode
                pluginOptions += getAPOptions().get()
            }

            pluginOptions += SubpluginOption("javacArguments", encodeList(javacOptions.get()))

            pluginOptions += SubpluginOption("includeCompileClasspath", includeCompileClasspath.toString())

            addMiscOptions(pluginOptions)

            pluginOptions
        }
    }

    private fun Kapt3SubpluginContext.getAPOptions(): Provider<CompositeSubpluginOption> = project.provider {
        val androidVariantData = KaptWithAndroid.androidVariantData(this)

        val annotationProcessorProviders = androidVariantData?.annotationProcessorOptionProviders

        val subluginOptionsFromProvidedApOptions = lazy {
            val apOptionsFromProviders =
                annotationProcessorProviders?.flatMap {
                    (it as CommandLineArgumentProvider).asArguments()
                }.orEmpty()

            apOptionsFromProviders.map {
                // Use the internal subplugin option type to exclude them from Gradle input/output checks, as their providers are already
                // properly registered as a nested input:

                // Pass options as they are in the key-only form (key = 'a=b'), kapt will deal with them:
                InternalSubpluginOption(key = it.removePrefix("-A"), value = "")
            }
        }

        CompositeSubpluginOption(
            "apoptions",
            lazy { encodeList((getDslKaptApOptions().get() + subluginOptionsFromProvidedApOptions.value).associate { it.key to it.value }) },
            getDslKaptApOptions().get()
        )
    }

    /* Returns AP options from static DSL. */
    private fun Kapt3SubpluginContext.getDslKaptApOptions(): Provider<List<SubpluginOption>> = project.provider {
        val androidVariantData = KaptWithAndroid.androidVariantData(this)

        val androidExtension = androidVariantData?.let {
            project.extensions.findByName("android") as? BaseExtension
        }

        val androidOptions = androidVariantData?.annotationProcessorOptions ?: emptyMap()
        val androidSubpluginOptions = androidOptions.toList().map { SubpluginOption(it.first, it.second) }

        androidSubpluginOptions + kaptExtension.getAdditionalArguments(project, androidVariantData, androidExtension).toList()
            .map { SubpluginOption(it.first, it.second) } +
                FilesSubpluginOption(KAPT_KOTLIN_GENERATED, listOf(kotlinSourcesOutputDir))
    }

    private fun Kapt3SubpluginContext.registerSubpluginOptions(
        taskProvider: TaskProvider<*>,
        optionsProvider: Provider<List<SubpluginOption>>
    ) {
        taskProvider.configure { taskInstance ->
            val container = when (taskInstance) {
                is KaptGenerateStubsTask -> taskInstance.pluginOptions
                is KaptWithKotlincTask -> taskInstance.pluginOptions
                is KaptWithoutKotlincTask -> taskInstance.processorOptions
                else -> error("Unexpected task ${taskInstance.name} (${taskInstance.javaClass})")
            }


            val compilerPluginId = getCompilerPluginId()

            val options = optionsProvider.get()

            taskInstance.registerSubpluginOptionsAsInputs(compilerPluginId, options)

            for (option in options) {
                container.addPluginArgument(compilerPluginId, option)
            }
        }

        // Also register all the subplugin options from the Kotlin task:
        project.whenEvaluated {
            taskProvider.configure { taskInstance ->
                kotlinCompile.get().pluginOptions.subpluginOptionsByPluginId.forEach { (pluginId, options) ->
                    taskInstance.registerSubpluginOptionsAsInputs("kotlinCompile.$pluginId", options)
                }
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
        pluginOptions += SubpluginOption("keepKdocCommentsInStubs", "${project.isKaptKeepKdocCommentsInStubs()}")
        pluginOptions += SubpluginOption("showProcessorTimings", "${kaptExtension.showProcessorTimings}")
        pluginOptions += SubpluginOption("detectMemoryLeaks", kaptExtension.detectMemoryLeaks)
        pluginOptions += SubpluginOption("infoAsWarnings", "${project.isInfoAsWarnings()}")
        pluginOptions += FilesSubpluginOption("stubs", listOf(getKaptStubsDir()))

        if (project.isKaptVerbose()) {
            pluginOptions += SubpluginOption("verbose", "true")
        }
    }

    private fun Kapt3SubpluginContext.createKaptKotlinTask(useWorkerApi: Boolean): TaskProvider<out KaptTask> {
        val taskClass = if (useWorkerApi) KaptWithoutKotlincTask::class.java else KaptWithKotlincTask::class.java
        val taskName = getKaptTaskName("kapt")

        var classStructureIfIncremental: Configuration? = null

        if (project.isIncrementalKapt()) {
            maybeRegisterTransform(project)

            classStructureIfIncremental = project.configurations.create("_classStructure${taskName}")

            // Wrap the `kotlinCompile.classpath` into a file collection, so that, if the classpath is represented by a configuration,
            // the configuration is not extended (via extendsFrom, which normally happens when one configuration is _added_ into another)
            // but is instead included as the (lazily) resolved files. This is needed because the class structure configuration doesn't have
            // the attributes that are potentially needed to resolve dependencies on MPP modules, and the classpath configuration does.
            project.dependencies.add(classStructureIfIncremental.name, project.files(project.provider { kotlinCompile.get().classpath }))
        }

        val kaptClasspathConfiguration = project.configurations.create("kaptClasspath_$taskName")
            .setExtendsFrom(kaptClasspathConfigurations).also {
                it.isVisible = false
                it.isCanBeConsumed = false
            }

        val kaptTaskProvider = project.registerTask(taskName, taskClass, emptyList()) { kaptTask ->
            kaptTask.useBuildCache = kaptExtension.useBuildCache

            val kotlinCompileTask = kotlinCompilation.compileKotlinTaskProvider.get() as KotlinCompile
            if (kaptTask is KaptWithoutKotlincTask) {
                KaptWithoutKotlincTask.Configurator(kotlinCompileTask).configure(kaptTask)
            } else {
                KaptWithKotlincTask.Configurator(kotlinCompileTask).configure(kaptTask as KaptWithKotlincTask)
                PropertiesProvider(project).mapKotlinDaemonProperties(kaptTask)
            }

            kaptTask.stubsDir.set(getKaptStubsDir())

            kaptTask.destinationDir = sourcesOutputDir
            kaptTask.kotlinSourcesDestinationDir = kotlinSourcesOutputDir
            kaptTask.classesDir = classesOutputDir
            kaptTask.includeCompileClasspath.set(includeCompileClasspath)

            kaptTask.isIncremental = project.isIncrementalKapt()

            if (kaptTask.isIncremental) {
                kaptTask.incAptCache.fileValue(getKaptIncrementalAnnotationProcessingCache()).disallowChanges()

                kaptTask.classpathStructure.from(
                    classStructureIfIncremental!!.incoming.artifactView { viewConfig ->
                        viewConfig.attributes.attribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
                    }.files
                ).disallowChanges()

                if (kaptTask is KaptWithKotlincTask) {
                    kaptTask.pluginOptions.addPluginArgument(
                        getCompilerPluginId(),
                        SubpluginOption(
                            "incrementalCache",
                            lazy { kaptTask.incAptCache.asFile.get().absolutePath }
                        )
                    )
                }
            }

            kaptTask.kaptClasspath.from(kaptClasspathConfiguration).disallowChanges()
            kaptTask.kaptExternalClasspath.from(kaptClasspathConfiguration.fileCollection { it is ExternalDependency })
            kaptTask.kaptClasspathConfigurationNames.value(kaptClasspathConfigurations.map { it.name }).disallowChanges()

            KaptWithAndroid.androidVariantData(this)?.annotationProcessorOptionProviders?.let {
                kaptTask.annotationProcessorOptionProviders.add(it)
            }
        }

        kotlinCompilation.output.classesDirs.from(kaptTaskProvider.map { it.classesDir })

        kotlinCompilation.compileKotlinTaskProvider.configure {
            it as SourceTask
            it.source(sourcesOutputDir, kotlinSourcesOutputDir)
        }

        if (javaCompile != null) {
            val androidVariantData = KaptWithAndroid.androidVariantData(this)
            if (androidVariantData != null) {
                KaptWithAndroid.registerGeneratedJavaSourceForAndroid(this, project, androidVariantData, kaptTaskProvider)
            } else {
                registerGeneratedJavaSource(kaptTaskProvider, javaCompile)
            }
        }

        val dslJavacOptions: Provider<Map<String, String>> = project.provider {
            kaptExtension.getJavacOptions().toMutableMap().also { result ->
                if (javaCompile != null && "-source" !in result && "--source" !in result && "--release" !in result) {
                    val atLeast12Java =
                        if (isConfigurationCacheAvailable(project.gradle)) {
                            val currentJavaVersion =
                                JavaVersion.parse(project.providers.systemProperty("java.version").forUseAtConfigurationTime().get())
                            currentJavaVersion.feature >= 12
                        } else {
                            SystemInfo.isJavaVersionAtLeast(12, 0, 0)
                        }
                    val sourceOptionKey = if (atLeast12Java) {
                        "--source"
                    } else {
                        "-source"
                    }
                    result[sourceOptionKey] = javaCompile.get().sourceCompatibility
                }
            }
        }

        if (KaptWithKotlincTask::class.java.isAssignableFrom(taskClass)) {
            val subpluginOptions = buildOptions("apt", dslJavacOptions)
            registerSubpluginOptions(kaptTaskProvider, subpluginOptions)
        }

        if (taskClass == KaptWithoutKotlincTask::class.java) {
            kaptTaskProvider.configure {
                it as KaptWithoutKotlincTask
                it.mapDiagnosticLocations = kaptExtension.mapDiagnosticLocations
                it.annotationProcessorFqNames = kaptExtension.processors.split(',').filter { it.isNotEmpty() }
                it.javacOptions = dslJavacOptions.get()
                if (includeCompileClasspath && project.classLoadersCacheSize() > 0) {
                    project.logger.warn(
                        "ClassLoaders cache can't be enabled together with AP discovery in compilation classpath."
                                + "\nSet 'kapt.includeCompileClasspath = false' to disable discovery"
                    )
                } else {
                    it.classLoadersCacheSize = project.classLoadersCacheSize()
                }
                it.disableClassloaderCacheForProcessors = project.disableClassloaderCacheForProcessors()
            }

            val subpluginOptions = getDslKaptApOptions()
            registerSubpluginOptions(kaptTaskProvider, subpluginOptions)
        }

        kaptTaskProvider.configure { task ->
            task.onlyIf {
                it as KaptTask
                it.includeCompileClasspath.get() || !it.kaptClasspath.isEmpty
            }
        }

        return kaptTaskProvider
    }

    private fun maybeRegisterTransform(project: Project) {
        if (!project.extensions.extraProperties.has("KaptStructureTransformAdded")) {
            val transformActionClass =
                if (GradleVersion.current() >= GradleVersion.version("5.4"))
                    StructureTransformAction::class.java
                else
                    StructureTransformLegacyAction::class.java
            project.dependencies.registerTransform(transformActionClass) { transformSpec ->
                transformSpec.from.attribute(artifactType, "jar")
                transformSpec.to.attribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
            }

            project.dependencies.registerTransform(transformActionClass) { transformSpec ->
                transformSpec.from.attribute(artifactType, "directory")
                transformSpec.to.attribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
            }

            project.extensions.extraProperties["KaptStructureTransformAdded"] = true
        }
    }

    private fun Kapt3SubpluginContext.createKaptGenerateStubsTask(): TaskProvider<KaptGenerateStubsTask> {
        val properties = PropertiesProvider(project)
        val kaptTaskName = getKaptTaskName("kaptGenerateStubs")
        val kaptTaskProvider = project.registerTask<KaptGenerateStubsTask>(kaptTaskName)

        val configurator = KaptGenerateStubsTask.Configurator(
            kotlinCompile,
            kotlinCompilation,
            properties,
            getKaptClasspathSnapshotDir()
        )
        configurator.runAtConfigurationTime(kaptTaskProvider, project)

        kaptTaskProvider.configure { kaptTask ->
            configurator.configure(kaptTask)

            kaptTask.stubsDir.set(getKaptStubsDir())
            kaptTask.destinationDirectory.set(getKaptIncrementalDataDir())
            kaptTask.generatedSourcesDirs = listOf(sourcesOutputDir, kotlinSourcesOutputDir)

            kaptTask.kaptClasspath.from(kaptClasspathConfigurations)

            properties.mapKotlinTaskProperties(kaptTask)

            if (!includeCompileClasspath) {
                kaptTask.onlyIf {
                    !(it as KaptGenerateStubsTask).kaptClasspath.isEmpty
                }
            }
        }

        project.whenEvaluated {
            addCompilationSourcesToExternalCompileTask(kotlinCompilation, kaptTaskProvider)
        }

        val subpluginOptions = buildOptions("stubs", project.provider { kaptExtension.getJavacOptions() })
        registerSubpluginOptions(kaptTaskProvider, subpluginOptions)

        return kaptTaskProvider
    }

    private fun Kapt3SubpluginContext.getKaptTaskName(prefix: String): String {
        // Replace compile*Kotlin to kapt*Kotlin
        val baseName = kotlinCompile.name
        assert(baseName.startsWith("compile"))
        return baseName.replaceFirst("compile", prefix)
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

private val artifactType = Attribute.of("artifactType", String::class.java)

// Don't reference the BaseVariant type in the Kapt plugin signatures, as those type references will fail to link when there's no Android
// Gradle plugin on the project's plugin classpath
private object KaptWithAndroid {
    // Avoid loading the BaseVariant type at call sites and instead lazily load it when evaluation reaches it in the body using inline:
    @Suppress("NOTHING_TO_INLINE")
    inline fun androidVariantData(context: Kapt3GradleSubplugin.Kapt3SubpluginContext): BaseVariant? = context.variantData as? BaseVariant

    @Suppress("NOTHING_TO_INLINE")
    // Avoid loading the BaseVariant type at call sites and instead lazily load it when evaluation reaches it in the body using inline:
    inline fun registerGeneratedJavaSourceForAndroid(
        kapt3SubpluginContext: Kapt3GradleSubplugin.Kapt3SubpluginContext,
        project: Project,
        variantData: BaseVariant,
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
        val generatedJavaSources = javaTask.project.fileTree(kaptTask.map { it.destinationDir })
        generatedJavaSources.include("**/*.java")
        javaTask.source(generatedJavaSources)
    }
}

internal fun Configuration.getNamedDependencies(): List<Dependency> = allDependencies.filter { it.group != null }

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

private val BaseVariant.annotationProcessorOptions: Map<String, String>?
    get() = javaCompileOptions.annotationProcessorOptions.arguments

private val BaseVariant.annotationProcessorOptionProviders: List<*>
    get() = try {
        // Public API added in Android Gradle Plugin 3.2.0-alpha15:
        val apOptions = javaCompileOptions.annotationProcessorOptions
        apOptions.javaClass.getMethod("getCompilerArgumentProviders").invoke(apOptions) as List<*>
    } catch (e: NoSuchMethodException) {
        emptyList<Any>()
    }

//TODO once the Android plugin reaches its 3.0.0 release, consider compiling against it (remove the reflective call)
private val BaseVariant.dataBindingDependencyArtifactsIfSupported: FileCollection?
    get() = this::class.java.methods
        .find { it.name == "getDataBindingDependencyArtifacts" }
        ?.also { it.isAccessible = true }
        ?.invoke(this) as? FileCollection

//region Stub implementation for legacy API, KT-39809
@Suppress("DEPRECATION_ERROR") // implementing to fix KT-39809
class Kapt3KotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    override fun isApplicable(project: Project, task: AbstractCompile): Boolean = false

    override fun apply(
        project: Project, kotlinCompile: AbstractCompile, javaCompile: AbstractCompile?, variantData: Any?, androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> = emptyList()

    override fun getCompilerPluginId(): String = Kapt3GradleSubplugin.KAPT_SUBPLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = JetBrainsSubpluginArtifact(artifactId = Kapt3GradleSubplugin.KAPT_ARTIFACT_NAME)
}
//endregion
