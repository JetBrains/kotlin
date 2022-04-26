/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.isInfoAsWarnings
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.isKaptKeepKdocCommentsInStubs
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.isKaptVerbose
import org.jetbrains.kotlin.gradle.model.builder.KaptModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.configuration.*
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
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

        val KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated"

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
                |Warning: '${BooleanOption.KAPT_USE_WORKER_API.optionName}' is deprecated and scheduled to be removed in Kotlin 1.8 release.
                |
                |By default Kapt plugin is using Gradle workers to run annotation processing. Running annotation processing
                |directly in the Kotlin compiler is deprecated.
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
                SingleWarningPerBuild.show(this, deprecationMessage())
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
                kaptGenerateStubsTask.additionalSources.from(
                    Callable {
                        // Avoid circular dependency: the stubs task need the Java sources, but the Java sources generated by Kapt should be
                        // excluded, as the Kapt tasks depend on the stubs ones, and having them in the input would lead to a cycle
                        val kaptJavaOutput = kaptTaskProvider.get().destinationDir.get().asFile
                        androidVariantData.getSourceFolders(SourceKind.JAVA).filter { it.dir != kaptJavaOutput }
                    }
                )
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
    private fun Kapt3SubpluginContext.buildOptionsForAptMode(
        javacOptions: Provider<Map<String, String>>
    ): Provider<List<SubpluginOption>> {
        return project.provider {
            buildKaptSubpluginOptions(
                kaptExtension,
                project,
                javacOptions.get(),
                "apt",
                generatedSourcesDir = listOf(sourcesOutputDir),
                generatedClassesDir = listOf(getKaptGeneratedClassesDir(project, sourceSetName)),
                incrementalDataDir = listOf(getKaptIncrementalDataDir()),
                includeCompileClasspath = includeCompileClasspath,
                kaptStubsDir = listOf(getKaptStubsDir())
            ) + getAPOptions().get() // apOptions are needed only for "apt" mode
        }
    }

    private fun Kapt3SubpluginContext.getAPOptions(): Provider<CompositeSubpluginOption> = project.provider {
        val androidVariantData = KaptWithAndroid.androidVariantData(this)

        val annotationProcessorProviders = androidVariantData?.annotationProcessorOptionProviders

        val subluginOptionsFromProvidedApOptions = lazy {
            val apOptionsFromProviders =
                annotationProcessorProviders
                    ?.flatMap { it.asArguments() }
                    .orEmpty()

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

        androidSubpluginOptions + getNonAndroidDslApOptions(
            kaptExtension, project, listOf(kotlinSourcesOutputDir), androidVariantData, androidExtension
        ).get()
    }

    private fun Kapt3SubpluginContext.createKaptKotlinTask(useWorkerApi: Boolean): TaskProvider<out KaptTask> {
        val taskClass = if (useWorkerApi) KaptWithoutKotlincTask::class.java else KaptWithKotlincTask::class.java
        val taskName = getKaptTaskName("kapt")

        val taskConfigAction = if (taskClass == KaptWithoutKotlincTask::class.java ) {
            KaptWithoutKotlincConfig(kotlinCompilation.compileKotlinTaskProvider.get() as KotlinCompile, kaptExtension)
        } else {
            KaptWithKotlincConfig(kotlinCompilation.compileKotlinTaskProvider.get() as KotlinCompile, kaptExtension)
        }

        val kaptClasspathConfiguration = project.configurations.create("kaptClasspath_$taskName")
            .setExtendsFrom(kaptClasspathConfigurations).also {
                it.isVisible = false
                it.isCanBeConsumed = false
            }
        taskConfigAction.configureTaskProvider { taskProvider ->
            if (javaCompile != null) {
                val androidVariantData = KaptWithAndroid.androidVariantData(this)
                if (androidVariantData != null) {
                    KaptWithAndroid.registerGeneratedJavaSourceForAndroid(this, project, androidVariantData, taskProvider)
                    androidVariantData.addJavaSourceFoldersToModel(sourcesOutputDir)
                } else {
                    registerGeneratedJavaSource(taskProvider, javaCompile)
                }

                disableAnnotationProcessingInJavaTask()
            }

            kotlinCompilation.output.classesDirs.from(taskProvider.flatMap { it.classesDir })

            kotlinCompilation.compileKotlinTaskProvider.configure {
                (it as AbstractKotlinCompileTool<*>).setSource(sourcesOutputDir, kotlinSourcesOutputDir)
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

            if (project.isIncrementalKapt()) {
                task.incAptCache.fileValue(getKaptIncrementalAnnotationProcessingCache()).disallowChanges()
            }

            task.kaptClasspath.from(kaptClasspathConfiguration).disallowChanges()
            task.kaptExternalClasspath.from(kaptClasspathConfiguration.fileCollection { it is ExternalDependency })
            task.kaptClasspathConfigurationNames.value(kaptClasspathConfigurations.map { it.name }).disallowChanges()

            KaptWithAndroid.androidVariantData(this)?.annotationProcessorOptionProviders?.let {
                task.annotationProcessorOptionProviders.add(it)
            }

            val pluginOptions: Provider<CompilerPluginOptions> = if (taskClass == KaptWithKotlincTask::class.java) {
                buildOptionsForAptMode(taskConfigAction.getJavaOptions(task.defaultJavaSourceCompatibility))
            } else {
                check(taskClass == KaptWithoutKotlincTask::class.java)
                getDslKaptApOptions()
            }.toCompilerPluginOptions()

            task.kaptPluginOptions.add(pluginOptions)
        }

        return if (taskClass == KaptWithoutKotlincTask::class.java) {
            taskConfigAction as KaptWithoutKotlincConfig
            project.registerTask(taskName, KaptWithoutKotlincTask::class.java, emptyList()).also {
                taskConfigAction.execute(it)
            }
        } else {
            taskConfigAction as KaptWithKotlincConfig
            project.registerTask(taskName, KaptWithKotlincTask::class.java, emptyList()).also {
                taskConfigAction.execute(it)
            }
        }
    }

    private fun Kapt3SubpluginContext.createKaptGenerateStubsTask(): TaskProvider<KaptGenerateStubsTask> {
        val kaptTaskName = getKaptTaskName("kaptGenerateStubs")
        val kaptTaskProvider = project.registerTask<KaptGenerateStubsTask>(kaptTaskName)
        val projectDir = project.projectDir

        val taskConfig = KaptGenerateStubsConfig(kotlinCompilation, kotlinCompile)
        taskConfig.configureTask {
            it.stubsDir.set(getKaptStubsDir())
            it.destinationDirectory.set(getKaptIncrementalDataDir())
            it.exclude(
                "${sourcesOutputDir.relativeTo(projectDir).path}/**",
                "${kotlinSourcesOutputDir.relativeTo(projectDir).path}/**"
            )
            it.kaptClasspath.from(kaptClasspathConfigurations)
        }

        taskConfig.execute(kaptTaskProvider)

        project.whenEvaluated {
            addCompilationSourcesToExternalCompileTask(kotlinCompilation, kaptTaskProvider)
        }

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

    val annotationProcessors = kaptExtension.processors
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
    pluginOptions += SubpluginOption("keepKdocCommentsInStubs", "${project.isKaptKeepKdocCommentsInStubs()}")
    pluginOptions += SubpluginOption("showProcessorTimings", "${kaptExtension.showProcessorStats}")
    pluginOptions += SubpluginOption("detectMemoryLeaks", kaptExtension.detectMemoryLeaks)
    pluginOptions += SubpluginOption("infoAsWarnings", "${project.isInfoAsWarnings()}")
    pluginOptions += FilesSubpluginOption("stubs", kaptStubsDir)

    if (project.isKaptVerbose()) {
        pluginOptions += SubpluginOption("verbose", "true")
    }

    return pluginOptions
}

/* Returns AP options from KAPT static DSL. */
internal fun getNonAndroidDslApOptions(
    kaptExtension: KaptExtension,
    project: Project,
    kotlinSourcesOutputDir: Iterable<File>,
    variantData: BaseVariant?,
    androidExtension: BaseExtension?
): Provider<List<SubpluginOption>> {
    return project.provider {
        kaptExtension.getAdditionalArguments(project, variantData, androidExtension).toList()
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
        val generatedJavaSources = javaTask.project.fileTree(kaptTask.flatMap { it.destinationDir })
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

private val BaseVariant.annotationProcessorOptionProviders: List<CommandLineArgumentProvider>
    get() = javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders

//TODO once the Android plugin reaches its 3.0.0 release, consider compiling against it (remove the reflective call)
private val BaseVariant.dataBindingDependencyArtifactsIfSupported: FileCollection?
    get() = this::class.java.methods
        .find { it.name == "getDataBindingDependencyArtifacts" }
        ?.also { it.isAccessible = true }
        ?.invoke(this) as? FileCollection
