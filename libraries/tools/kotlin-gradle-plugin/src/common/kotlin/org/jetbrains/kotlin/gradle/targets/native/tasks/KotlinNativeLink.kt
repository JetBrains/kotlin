/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Action
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel
import org.jetbrains.kotlin.compilerRunner.addBuildMetricsForTaskAction
import org.jetbrains.kotlin.compilerRunner.getKonanCacheKind
import org.jetbrains.kotlin.compilerRunner.getKonanCacheOrchestration
import org.jetbrains.kotlin.compilerRunner.getKonanParallelThreads
import org.jetbrains.kotlin.compilerRunner.isKonanIncrementalCompilationEnabled
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.UsesClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.create
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.useXcodeMessageStyle
import org.jetbrains.kotlin.gradle.plugin.statistics.UsesBuildFusService
import org.jetbrains.kotlin.gradle.report.UsesBuildMetricsService
import org.jetbrains.kotlin.gradle.targets.native.UsesKonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.targets.native.tasks.CompilerPluginData
import org.jetbrains.kotlin.gradle.targets.native.toolchain.NoopKotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.UsesKotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeCompilerRunner
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeToolRunner
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.project.model.LanguageSettings
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File
import javax.inject.Inject

/**
 * A task producing a final binary from a compilation.
 */
@CacheableTask
abstract class KotlinNativeLink
@Inject
constructor(
    @Internal
    @Transient // This property can't be accessed in the execution phase
    val binary: NativeBinary,
    private val objectFactory: ObjectFactory,
) : AbstractKotlinCompileTool<K2NativeCompilerArguments>(objectFactory),
    UsesKonanPropertiesBuildService,
    UsesBuildMetricsService,
    UsesClassLoadersCachingBuildService,
    KotlinToolTask<KotlinCommonCompilerToolOptions>,
    UsesKotlinNativeBundleBuildService,
    UsesBuildFusService
{

    @Deprecated("Visibility will be lifted to private in the future releases")
    @get:Internal
    val compilation: KotlinNativeCompilation
        get() = binary.compilation

    final override val toolOptions: KotlinCommonCompilerToolOptions = objectFactory
        .newInstance<KotlinCommonCompilerToolOptionsDefault>()

    override val destinationDirectory: DirectoryProperty = binary.outputDirectoryProperty

    @get:Classpath
    override val libraries: ConfigurableFileCollection = objectFactory.fileCollection().from(
        {
            // Avoid resolving these dependencies during task graph construction when we can't build the target:
            @Suppress("DEPRECATION")
            if (konanTarget.enabledOnCurrentHostForBinariesCompilation()) compilation.compileDependencyFiles
            else objectFactory.fileCollection()
        }
    )

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal var excludeOriginalPlatformLibraries: FileCollection? = null

    @get:Input
    val outputKind: CompilerOutputKind by lazyConvention { binary.outputKind.compilerOutputKind }

    @get:Input
    val optimized: Boolean by lazyConvention { binary.optimized }

    @get:Input
    val debuggable: Boolean by lazyConvention { binary.debuggable }

    @get:Input
    val baseName: String by lazyConvention { binary.baseName }

    @get:Input
    internal val binaryName: String by lazyConvention { binary.name }

    @Suppress("DEPRECATION")
    @get:Internal
    internal val konanTarget = compilation.konanTarget

    @Suppress("DEPRECATION")
    @Deprecated("Use toolOptions to configure the task")
    @get:Internal
    val languageSettings: LanguageSettings = compilation.defaultSourceSet.languageSettings

    @Suppress("unused")
    @get:Input
    protected val konanCacheKind: Provider<NativeCacheKind> = project.getKonanCacheKind(konanTarget)

    @Suppress("unused")
    @get:Input
    internal val useEmbeddableCompilerJar: Provider<Boolean> = project.nativeProperties.shouldUseEmbeddableCompilerJar

    @Suppress("unused", "UNCHECKED_CAST")
    @Deprecated(
        "Use toolOptions.freeCompilerArgs",
        replaceWith = ReplaceWith("toolOptions.freeCompilerArgs.get()")
    )
    @get:Internal
    val additionalCompilerOptions: Provider<Collection<String>> = toolOptions.freeCompilerArgs as Provider<Collection<String>>

    @Suppress("DEPRECATION")
    @Deprecated(KOTLIN_OPTIONS_AS_TOOLS_DEPRECATION_MESSAGE)
    @get:Internal
    val kotlinOptions: KotlinCommonToolOptions = object : KotlinCommonToolOptions {
        override val options: KotlinCommonCompilerToolOptions
            get() = toolOptions
    }

    @Deprecated(KOTLIN_OPTIONS_AS_TOOLS_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    @Deprecated(KOTLIN_OPTIONS_AS_TOOLS_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    fun kotlinOptions(fn: Action<KotlinCommonToolOptions>) {
        fn.execute(kotlinOptions)
    }

    // Binary-specific options.
    private val _entryPoint: String by lazyConvention { (binary as? Executable)?.entryPoint.orEmpty() }

    @get:Input
    @get:Optional
    val entryPoint: String?
        get() = _entryPoint.ifEmpty { null }

    @get:Input
    val linkerOpts: List<String> by lazyConvention { binary.linkerOpts }

    @get:Input
    internal val additionalLinkerOpts: MutableList<String> = mutableListOf()

    @get:Input
    val binaryOptions: Map<String, String> by lazy { PropertiesProvider(project).nativeBinaryOptions + binary.binaryOptions }

    @get:Input
    val processTests: Boolean by lazyConvention { binary is TestExecutable }

    @get:Classpath
    val exportLibraries: FileCollection get() = exportLibrariesResolvedConfiguration?.files ?: objectFactory.fileCollection()

    private val exportLibrariesResolvedConfiguration = if (binary is AbstractNativeLibrary) {
        LazyResolvedConfiguration(project.configurations.maybeCreateResolvable(binary.exportConfigurationName))
    } else {
        null
    }

    @get:Input
    val isStaticFramework: Boolean by lazyConvention { binary.let { it is Framework && it.isStatic } }

    @Suppress("DEPRECATION")
    @get:Input
    val target: String = compilation.konanTarget.name

    @Suppress("DEPRECATION")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, replaceWith = ReplaceWith(""))
    @get:Internal
    val embedBitcode: BitcodeEmbeddingMode
        get() = embedBitcodeMode.get()

    @get:Input
    @get:Optional
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE)
    val embedBitcodeMode: Provider<BitcodeEmbeddingMode> = objectFactory.property()

    @get:Internal
    val apiFiles: ConfigurableFileCollection = objectFactory.fileCollection()

    private val externalDependenciesArgs by lazy {
        @Suppress("DEPRECATION")
        ExternalDependenciesBuilder(project, compilation).buildCompilerArgs()
    }

    private val cacheBuilderSettings
        get() = CacheBuilder.Settings(
            konanHome = kotlinNativeProvider.flatMap { it.bundleDirectory.asFile },
            konanCacheKind = project.getKonanCacheKind(konanTarget),
            gradleUserHomeDir = project.gradle.gradleUserHomeDir,
            konanTarget = konanTarget,
            toolOptions = toolOptions,
            externalDependenciesArgs = externalDependenciesArgs,
            debuggable = binary.debuggable,
            optimized = binary.optimized,
            konanDataDir = kotlinNativeProvider.flatMap { it.konanDataDir.map { File(it) } },
            kotlinCompilerArgumentsLogLevel = kotlinCompilerArgumentsLogLevel,
            forceDisableRunningInProcess = forceDisableRunningInProcess,
        )

    private class CacheSettings(
        val orchestration: NativeCacheOrchestration, val kind: NativeCacheKind,
        val icEnabled: Boolean, val threads: Int,
        val gradleUserHomeDir: File, val gradleBuildDir: File,
    )

    private val cacheSettings by lazy {
        CacheSettings(
            project.getKonanCacheOrchestration(), project.getKonanCacheKind(konanTarget).get(),
            project.isKonanIncrementalCompilationEnabled(), project.getKonanParallelThreads(),
            project.gradle.gradleUserHomeDir, project.layout.buildDirectory.get().asFile
        )
    }

    override fun createCompilerArguments(context: CreateCompilerArgumentsContext) = context.create<K2NativeCompilerArguments> {
        val compilerPlugins = listOfNotNull(
            compilerPluginClasspath?.let { CompilerPluginData(it, compilerPluginOptions) },
            kotlinPluginData?.orNull?.let { CompilerPluginData(it.classpath, it.options) }
        )

        primitive { args ->
            args.outputName = outputFile.get().absolutePath
            args.optimization = optimized
            args.debug = debuggable
            args.enableAssertions = debuggable
            args.target = konanTarget.name
            args.produce = outputKind.name.toLowerCaseAsciiOnly()
            args.multiPlatform = true
            args.noendorsedlibs = true
            args.nostdlib = true
            args.pluginOptions = compilerPlugins.flatMap { it.options.arguments }.toTypedArray()
            args.generateTestRunner = processTests
            args.mainPackage = entryPoint
            args.singleLinkerArguments = (linkerOpts + additionalLinkerOpts).toTypedArray()
            args.binaryOptions = binaryOptions.map { (key, value) -> "$key=$value" }.toTypedArray()
            args.staticFramework = isStaticFramework
            args.konanDataDir = kotlinNativeProvider.get().konanDataDir.orNull

            KotlinCommonCompilerToolOptionsHelper.fillCompilerArguments(toolOptions, args)
        }

        pluginClasspath { args ->
            args.pluginClasspaths = compilerPlugins.flatMap { classpath -> runSafe { classpath.files } ?: emptySet() }.toPathsArray()
        }

        dependencyClasspath { args ->
            args.libraries = runSafe {
                libraries.exclude(excludeOriginalPlatformLibraries).files.filterKlibsPassedToCompiler()
            }?.toPathsArray()
            args.exportedLibraries = runSafe { exportLibraries.files.filterKlibsPassedToCompiler() }?.toPathsArray()
            args.friendModules = runSafe { friendModule.files.toList().takeIf { it.isNotEmpty() } }
                ?.joinToString(File.pathSeparator) { it.absolutePath }
        }

        sources { args ->
            args.includes = sourceFiles.files.toPathsArray()
        }
    }

    private fun validatedExportedLibraries() {
        if (exportLibrariesResolvedConfiguration == null) return

        val failed = mutableSetOf<ResolvedDependencyResult>()
        exportLibrariesResolvedConfiguration
            .allDependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .forEach {
                val dependencyFiles = exportLibrariesResolvedConfiguration.getArtifacts(it).map { it.file }.filterKlibsPassedToCompiler()
                if (!apiFiles.files.containsAll(dependencyFiles)) {
                    failed.add(it)
                }
            }

        check(failed.isEmpty()) {
            val failedDependenciesList = failed.joinToString(separator = "\n") {
                val componentId = it.selected.id
                when (componentId) {
                    is ModuleComponentIdentifier -> "|Files: ${exportLibrariesResolvedConfiguration.getArtifacts(it).map { it.file }}"
                    is ProjectComponentIdentifier -> "|Project ${componentId.projectPath}"
                    else -> "|${componentId.displayName}"
                }
            }

            """
                |Following dependencies exported in the $binaryName binary are not specified as API-dependencies of a corresponding source set:
                |
                $failedDependenciesList
                |
                |Please add them in the API-dependencies and rerun the build.
            """.trimMargin()
        }
    }

    @Suppress("DEPRECATION")
    @get:Classpath
    protected val friendModule: FileCollection = objectFactory.fileCollection().from({ compilation.friendPaths })

    @Suppress("DEPRECATION")
    private val resolvedConfiguration = LazyResolvedConfiguration(
        project.configurations.getByName(compilation.compileDependencyConfigurationName)
    )

    @get:Internal
    open val outputFile: Provider<File>
        get() = destinationDirectory.flatMap {
            val prefix = outputKind.prefix(konanTarget)
            val suffix = outputKind.suffix(konanTarget)
            val filename = "$prefix${baseName}$suffix".let {
                when {
                    outputKind == CompilerOutputKind.FRAMEWORK ->
                        it.asValidFrameworkName()
                    outputKind in listOf(CompilerOutputKind.STATIC, CompilerOutputKind.DYNAMIC) ->
                        it.replace('-', '_')
                    else -> it
                }
            }

            objectFactory.property(it.file(filename).asFile)
        }

    @Internal
    val compilerPluginOptions = CompilerPluginOptions()

    @Optional
    @Classpath
    open var compilerPluginClasspath: FileCollection? = null

    /**
     * Plugin Data provided by [KpmCompilerPlugin]
     */
    @get:Optional
    @get:Nested
    var kotlinPluginData: Provider<KotlinCompilerPluginData>? = null

    @get:Nested
    internal val kotlinNativeProvider: Property<KotlinNativeProvider> =
        project.objects.propertyWithConvention<KotlinNativeProvider>(
            // For KT-66452 we need to get rid of invocation of 'Task.project'.
            // That is why we moved setting this property to task registration
            // and added convention for backwards compatibility.
            NoopKotlinNativeProvider(project)
        )

    @Deprecated(
        message = "This property will be removed in future releases. Don't use it in your code.",
    )
    @get:Internal
    val konanDataDir: Provider<String?> = kotlinNativeProvider.flatMap { it.konanDataDir }

    @Deprecated(
        message = "This property will be removed in future releases. Don't use it in your code.",
    )
    @get:Internal
    val konanHome: Provider<String> = kotlinNativeProvider.map { it.bundleDirectory.get().asFile.absolutePath }

    @get:Internal
    internal abstract val kotlinCompilerArgumentsLogLevel: Property<KotlinCompilerArgumentsLogLevel>

    private val actualNativeHomeDirectory = project.nativeProperties.actualNativeHomeDirectory
    private val runnerJvmArgs = project.nativeProperties.jvmArgs
    private val forceDisableRunningInProcess = project.nativeProperties.forceDisableRunningInProcess
    private val useXcodeMessageStyle = project.useXcodeMessageStyle

    @get:Internal
    internal val nativeCompilerRunner
        get() = objectFactory.KotlinNativeCompilerRunner(
            metrics,
            classLoadersCachingService,
            forceDisableRunningInProcess,
            useXcodeMessageStyle,
            useEmbeddableCompilerJar,
            actualNativeHomeDirectory,
            runnerJvmArgs,
            konanPropertiesService,
            buildFusService
        )

    @TaskAction
    fun compile() {
        val metricsReporter = metrics.get()

        addBuildMetricsForTaskAction(metricsReporter = metricsReporter, languageVersion = null) {
            validatedExportedLibraries()

            val output = outputFile.get()
            output.parentFile.mkdirs()

            val additionalOptions = mutableListOf<String>().apply {
                addAll(externalDependenciesArgs)
                when (cacheSettings.orchestration) {
                    NativeCacheOrchestration.Compiler -> {
                        if (cacheSettings.kind != NativeCacheKind.NONE
                            && !optimized
                            && konanPropertiesService.get().cacheWorksFor(konanTarget)
                        ) {
                            add("-Xauto-cache-from=${cacheSettings.gradleUserHomeDir}")
                            add("-Xbackend-threads=${cacheSettings.threads}")
                            if (cacheSettings.icEnabled) {
                                val icCacheDir = cacheSettings.gradleBuildDir
                                    .resolve("kotlin-native-ic-cache")
                                    .resolve(binaryName)
                                icCacheDir.mkdirs()
                                add("-Xenable-incremental-compilation")
                                add("-Xic-cache-dir=$icCacheDir")
                            }
                        }
                    }
                    NativeCacheOrchestration.Gradle -> {
                        if (cacheSettings.icEnabled) {
                            logger.warn(
                                "K/N incremental compilation only works in conjunction with kotlin.native.cacheOrchestration=compiler"
                            )
                        }
                        val cacheBuilder = CacheBuilder(
                            settings = cacheBuilderSettings,
                            konanPropertiesService = konanPropertiesService.get(),
                            nativeCompilerRunner = nativeCompilerRunner,
                        )
                        addAll(cacheBuilder.buildCompilerArgs(resolvedConfiguration))
                    }
                }
            }

            val arguments = createCompilerArguments()
            val buildArguments = ArgumentUtils.convertArgumentsToStringList(arguments) + additionalOptions

            nativeCompilerRunner.runTool(
                KotlinNativeToolRunner.ToolArguments(
                    shouldRunInProcessMode = !forceDisableRunningInProcess.get(),
                    compilerArgumentsLogLevel = kotlinCompilerArgumentsLogLevel.get(),
                    arguments = buildArguments
                )
            )
        }
    }

    private inline fun <reified T : Any> lazyConvention(noinline lazyConventionValue: () -> T): Provider<T> {
        return objectFactory.providerWithLazyConvention(lazyConventionValue)
    }
}
