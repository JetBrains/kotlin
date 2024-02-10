/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import groovy.lang.Closure
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.create
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.report.UsesBuildMetricsService
import org.jetbrains.kotlin.gradle.targets.native.UsesKonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.targets.native.tasks.CompilerPluginData
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.UsesKotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
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
    private val execOperations: ExecOperations,
) : AbstractKotlinCompileTool<K2NativeCompilerArguments>(objectFactory),
    UsesKonanPropertiesBuildService,
    UsesBuildMetricsService,
    KotlinToolTask<KotlinCommonCompilerToolOptions>,
    UsesKotlinNativeBundleBuildService {

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
            if (konanTarget.enabledOnCurrentHost) compilation.compileDependencyFiles
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
    private val konanTarget = compilation.konanTarget

    @Suppress("DEPRECATION")
    @Deprecated("Use toolOptions to configure the task")
    @get:Internal
    val languageSettings: LanguageSettings = compilation.defaultSourceSet.languageSettings

    @Suppress("unused")
    @get:Input
    protected val konanCacheKind: Provider<NativeCacheKind> = objectFactory.providerWithLazyConvention {
        project.getKonanCacheKind(konanTarget)
    }

    @Suppress("unused")
    @get:Input
    internal val useEmbeddableCompilerJar: Boolean = project.nativeUseEmbeddableCompilerJar

    @Suppress("unused", "UNCHECKED_CAST")
    @Deprecated(
        "Use toolOptions.freeCompilerArgs",
        replaceWith = ReplaceWith("toolOptions.freeCompilerArgs.get()")
    )
    @get:Internal
    val additionalCompilerOptions: Provider<Collection<String>> = toolOptions.freeCompilerArgs as Provider<Collection<String>>

    @get:Internal
    val kotlinOptions: KotlinCommonToolOptions = object : KotlinCommonToolOptions {
        override val options: KotlinCommonCompilerToolOptions
            get() = toolOptions
    }

    fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    fun kotlinOptions(fn: Closure<*>) {
        fn.delegate = kotlinOptions
        fn.call()
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
        LazyResolvedConfiguration(project.configurations.getByName(binary.exportConfigurationName))
    } else {
        null
    }

    @get:Input
    val isStaticFramework: Boolean by lazyConvention { binary.let { it is Framework && it.isStatic } }

    @Suppress("DEPRECATION")
    @get:Input
    val target: String = compilation.konanTarget.name

    @Deprecated("Use 'embedBitcodeMode' provider instead.", ReplaceWith("embedBitcodeMode.get()"))
    @get:Internal
    val embedBitcode: BitcodeEmbeddingMode
        get() = embedBitcodeMode.get()

    @get:Input
    @get:Optional
    val embedBitcodeMode: Provider<BitcodeEmbeddingMode> =
        (binary as? Framework)?.embedBitcodeMode ?: objectFactory.property()

    @get:Internal
    val apiFiles = project
        .configurations
        .detachedResolvable()
        .apply @Suppress("DEPRECATION") {
            val apiConfiguration = project.configurations.getByName(compilation.apiConfigurationName)
            dependencies.addAll(apiConfiguration.allDependencies)
            usesPlatformOf(compilation.target)
            attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(compilation.target))
            attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        }.filterKlibsPassedToCompiler()

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val xcodeVersion = objectFactory.fileProperty()

    private val externalDependenciesArgs by lazy {
        @Suppress("DEPRECATION")
        ExternalDependenciesBuilder(project, compilation).buildCompilerArgs()
    }

    private val cacheBuilderSettings by lazy {
        CacheBuilder.Settings.createWithProject(
            kotlinNativeProvider.get().bundleDirectory.getFile().absolutePath,
            kotlinNativeProvider.get().konanDataDir.orNull,
            project,
            binary,
            konanTarget,
            toolOptions,
            externalDependenciesArgs
        )
    }

    private class CacheSettings(
        val orchestration: NativeCacheOrchestration, val kind: NativeCacheKind,
        val icEnabled: Boolean, val threads: Int,
        val gradleUserHomeDir: File, val gradleBuildDir: File,
    )

    private val cacheSettings by lazy {
        CacheSettings(
            project.getKonanCacheOrchestration(), project.getKonanCacheKind(konanTarget),
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

            when (bitcodeEmbeddingMode()) {
                BitcodeEmbeddingMode.BITCODE -> args.embedBitcode = true
                BitcodeEmbeddingMode.MARKER -> args.embedBitcodeMarker = true
                BitcodeEmbeddingMode.DISABLE -> Unit
            }

            args.singleLinkerArguments = (linkerOpts + additionalLinkerOpts).toTypedArray()
            args.binaryOptions = binaryOptions.map { (key, value) -> "$key=$value" }.toTypedArray()
            args.staticFramework = isStaticFramework

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
            args.includes = sources.asFileTree.files.toPathsArray()
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
                    outputKind in listOf(CompilerOutputKind.STATIC, CompilerOutputKind.DYNAMIC) ||
                            outputKind == CompilerOutputKind.PROGRAM && konanTarget == KonanTarget.WASM32 ->
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
    internal val kotlinNativeProvider: Provider<KotlinNativeProvider> = project.provider {
        KotlinNativeProvider(project, konanTarget, kotlinNativeBundleBuildService)
    }

    @Deprecated(
        message = "This property as a konanHome will be squashed into one in future releases.",
        replaceWith = ReplaceWith("kotlinNativeProvider.konanDataDir")
    )
    @get:Internal
    val konanDataDir: Provider<String?> = kotlinNativeProvider.flatMap { it.konanDataDir }

    @Deprecated(
        message = "This property as a konanDataDir will be squashed into one in future releases.",
        replaceWith = ReplaceWith("kotlinNativeProvider.compilerDirectory")
    )
    @get:Internal
    val konanHome: Provider<String> = kotlinNativeProvider.map { it.bundleDirectory.get().asFile.absolutePath }

    private val runnerSettings = KotlinNativeCompilerRunner.Settings.of(
        kotlinNativeProvider.get().bundleDirectory.getFile().absolutePath,
        kotlinNativeProvider.get().konanDataDir.orNull,
        project
    )

    @TaskAction
    fun compile() {
        val metricsReporter = metrics.get()

        addBuildMetricsForTaskAction(metricsReporter = metricsReporter, languageVersion = null) {
            validatedExportedLibraries()

            val output = outputFile.get()
            output.parentFile.mkdirs()

            val executionContext = KotlinToolRunner.GradleExecutionContext.fromTaskContext(objectFactory, execOperations, logger)
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
                            executionContext.logger.warn(
                                "K/N incremental compilation only works in conjunction with kotlin.native.cacheOrchestration=compiler"
                            )
                        }
                        val cacheBuilder = CacheBuilder(
                            executionContext = executionContext,
                            settings = cacheBuilderSettings,
                            konanPropertiesService = konanPropertiesService.get(),
                            metricsReporter = metricsReporter
                        )
                        addAll(cacheBuilder.buildCompilerArgs(resolvedConfiguration))
                    }
                }
            }

            val arguments = createCompilerArguments()
            val buildArguments = ArgumentUtils.convertArgumentsToStringList(arguments) + additionalOptions

            KotlinNativeCompilerRunner(
                settings = runnerSettings,
                executionContext = executionContext,
                metricsReporter = metricsReporter

            ).run(buildArguments)
        }
    }

    private inline fun <reified T : Any> lazyConvention(noinline lazyConventionValue: () -> T): Provider<T> {
        return objectFactory.providerWithLazyConvention(lazyConventionValue)
    }

    private fun bitcodeEmbeddingMode(): BitcodeEmbeddingMode {
        return XcodeUtils.bitcodeEmbeddingMode(outputKind, embedBitcodeMode.orNull, xcodeVersion, konanTarget, debuggable)
    }
}
