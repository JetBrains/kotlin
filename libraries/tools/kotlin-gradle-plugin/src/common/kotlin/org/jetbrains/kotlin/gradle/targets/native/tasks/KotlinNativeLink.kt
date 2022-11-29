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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCompilerRunner
import org.jetbrains.kotlin.compilerRunner.KotlinToolRunner
import org.jetbrains.kotlin.compilerRunner.getKonanCacheKind
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.targets.native.tasks.CompilerPluginData
import org.jetbrains.kotlin.gradle.targets.native.tasks.buildKotlinNativeBinaryLinkerArgs
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.project.model.LanguageSettings
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
    val binary: NativeBinary,
    private val objectFactory: ObjectFactory,
    private val execOperations: ExecOperations
) : AbstractKotlinCompileTool<StubK2NativeCompilerArguments>(objectFactory),
    KotlinToolTask<KotlinCommonCompilerToolOptions> {
    @Deprecated("Visibility will be lifted to private in the future releases")
    @get:Internal
    val compilation: KotlinNativeCompilation
        get() = binary.compilation

    private val runnerSettings = KotlinNativeCompilerRunner.Settings.fromProject(project)

    final override val toolOptions: KotlinCommonCompilerToolOptions = objectFactory
        .newInstance<KotlinCommonCompilerToolOptionsDefault>()
        .apply {
            freeCompilerArgs.addAll(PropertiesProvider(project).nativeLinkArgs)
        }

    init {
        @Suppress("DEPRECATION")
        this.dependsOn(compilation.compileTaskProvider)
        // Frameworks actively uses symlinks.
        // Gradle build cache transforms symlinks into regular files https://guides.gradle.org/using-build-cache/#symbolic_links
        outputs.cacheIf { outputKind != CompilerOutputKind.FRAMEWORK }

        @Suppress("DEPRECATION")
        this.setSource(compilation.compileTaskProvider.map { it.outputFile })
        includes.clear() // we need to include non '.kt' or '.kts' files
        disallowSourceChanges()
    }

    override val destinationDirectory: DirectoryProperty = binary.outputDirectoryProperty

    @get:Classpath
    override val libraries: ConfigurableFileCollection = objectFactory.fileCollection().from(
        {
            // Avoid resolving these dependencies during task graph construction when we can't build the target:
            @Suppress("DEPRECATION")
            if (konanTarget.enabledOnCurrentHost)
                objectFactory.fileCollection().from(
                    compilation.compileDependencyFiles.filterOutPublishableInteropLibs(project)
                )
            else objectFactory.fileCollection()
        }
    )

    @get:Input
    val outputKind: CompilerOutputKind get() = binary.outputKind.compilerOutputKind

    @get:Input
    val optimized: Boolean get() = binary.optimized

    @get:Input
    val debuggable: Boolean get() = binary.debuggable

    @get:Input
    val baseName: String get() = binary.baseName

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

    @get:Input
    internal val useEmbeddableCompilerJar: Boolean
        get() = project.nativeUseEmbeddableCompilerJar

    @Suppress("unused", "UNCHECKED_CAST")
    @Deprecated(
        "Use toolOptions.freeCompilerArgs",
        replaceWith = ReplaceWith("toolOptions.freeCompilerArgs")
    )
    @get:Internal
    val additionalCompilerOptions: Provider<Collection<String>> = toolOptions.freeCompilerArgs as Provider<Collection<String>>

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Replaced with toolOptions",
        replaceWith = ReplaceWith("toolOptions")
    )
    @get:Internal
    val kotlinOptions: KotlinCommonToolOptions = object : KotlinCommonToolOptions {
        override val options: KotlinCommonCompilerToolOptions
            get() = toolOptions
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Replaced with toolOptions()",
        replaceWith = ReplaceWith("toolOptions(fn)")
    )
    fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    @Deprecated(
        message = "Replaced with toolOptions()",
        replaceWith = ReplaceWith("toolOptions(fn)")
    )
    fun kotlinOptions(fn: Closure<*>) {
        @Suppress("DEPRECATION")
        fn.delegate = kotlinOptions
        fn.call()
    }

    // Binary-specific options.
    @get:Input
    @get:Optional
    val entryPoint: String? get() = (binary as? Executable)?.entryPoint

    @get:Input
    val linkerOpts: List<String> get() = binary.linkerOpts

    @get:Input
    val binaryOptions: Map<String, String> by lazy { PropertiesProvider(project).nativeBinaryOptions + binary.binaryOptions }

    @get:Input
    val processTests: Boolean get() = binary is TestExecutable

    @get:Classpath
    val exportLibraries: FileCollection get() = exportLibrariesResolvedGraph?.files ?: objectFactory.fileCollection()

    private val exportLibrariesResolvedGraph = if (binary is AbstractNativeLibrary) {
        ResolvedDependencyGraph(project.configurations.getByName(binary.exportConfigurationName))
    } else {
        null
    }

    @get:Input
    val isStaticFramework: Boolean
        get() = binary.let { it is Framework && it.isStatic }

    @Suppress("DEPRECATION")
    @get:Input
    val target: String = compilation.konanTarget.name

    @Deprecated("Use 'embedBitcodeMode' provider instead.", ReplaceWith("embedBitcodeMode.get()"))
    @get:Internal
    val embedBitcode: BitcodeEmbeddingMode
        get() = embedBitcodeMode.get()

    @get:Input
    val embedBitcodeMode: Provider<BitcodeEmbeddingMode> =
        (binary as? Framework)?.embedBitcodeMode ?: project.provider { BitcodeEmbeddingMode.DISABLE }

    @get:Internal
    val apiFiles = project.files(project.configurations.getByName(compilation.apiConfigurationName)).filterKlibsPassedToCompiler()

    private val externalDependenciesArgs by lazy { ExternalDependenciesBuilder(project, compilation).buildCompilerArgs() }

    private val cacheBuilderSettings by lazy {
        CacheBuilder.Settings.createWithProject(project, binary, konanTarget, toolOptions, externalDependenciesArgs)
    }

    override fun createCompilerArgs(): StubK2NativeCompilerArguments = StubK2NativeCompilerArguments()

    override fun setupCompilerArgs(
        args: StubK2NativeCompilerArguments,
        defaultsOnly: Boolean,
        ignoreClasspathResolutionErrors: Boolean
    ) = Unit

    private fun validatedExportedLibraries() {
        if (exportLibrariesResolvedGraph == null) return

        val failed = mutableSetOf<ResolvedDependencyResult>()
        exportLibrariesResolvedGraph
            .allDependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .forEach {
                val dependencyFiles = exportLibrariesResolvedGraph.dependencyArtifacts(it).map { it.file }.filterKlibsPassedToCompiler()
                if (!apiFiles.files.containsAll(dependencyFiles)) {
                    failed.add(it)
                }
            }

        check(failed.isEmpty()) {
            val failedDependenciesList = failed.joinToString(separator = "\n") {
                val componentId = it.selected.id
                when (componentId) {
                    is ModuleComponentIdentifier -> "|Files: ${exportLibrariesResolvedGraph.dependencyArtifacts(it).map { it.file }}"
                    is ProjectComponentIdentifier -> "|Project ${componentId.projectPath}"
                    else -> "|${componentId.displayName}"
                }
            }

            """
                |Following dependencies exported in the ${binary.name} binary are not specified as API-dependencies of a corresponding source set:
                |
                $failedDependenciesList
                |
                |Please add them in the API-dependencies and rerun the build.
            """.trimMargin()
        }
    }

    @get:Internal
    internal abstract val konanPropertiesService: Property<KonanPropertiesBuildService>

    @Suppress("DEPRECATION")
    @get:Classpath
    protected val friendModule: FileCollection = objectFactory.fileCollection().from({ compilation.friendPaths })

    @Suppress("DEPRECATION")
    private val resolvedDependencyGraph = ResolvedDependencyGraph(
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

    @Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")
    @Deprecated("Please declare explicit dependency on kotlinx-cli. This option is scheduled to be removed in 1.9.0")
    @get:Input
    val enableEndorsedLibs: Boolean by lazy { compilation.enableEndorsedLibs }

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

    @TaskAction
    fun compile() {
        validatedExportedLibraries()

        val output = outputFile.get()
        output.parentFile.mkdirs()

        val plugins = listOfNotNull(
            compilerPluginClasspath?.let { CompilerPluginData(it, compilerPluginOptions) },
            kotlinPluginData?.orNull?.let { CompilerPluginData(it.classpath, it.options) }
        )

        val executionContext = KotlinToolRunner.GradleExecutionContext.fromTaskContext(objectFactory, execOperations, logger)
        val cacheArgs = CacheBuilder(
            executionContext = executionContext,
            settings = cacheBuilderSettings,
            konanPropertiesService = konanPropertiesService.get()
        ).buildCompilerArgs(resolvedDependencyGraph)

        @Suppress("DEPRECATION") val enableEndorsedLibs = this.enableEndorsedLibs // TODO: remove before 1.9.0, see KT-54098

        val buildArgs = buildKotlinNativeBinaryLinkerArgs(
            output,
            optimized,
            debuggable,
            konanTarget,
            outputKind,
            libraries.files.filterKlibsPassedToCompiler(),
            friendModule.files.toList(),
            enableEndorsedLibs,
            toolOptions,
            plugins,
            processTests,
            entryPoint,
            embedBitcodeMode.get(),
            linkerOpts,
            binaryOptions,
            isStaticFramework,
            exportLibraries.files.filterKlibsPassedToCompiler(),
            sources.asFileTree.files.toList(),
            externalDependenciesArgs + cacheArgs
        )

        KotlinNativeCompilerRunner(
            settings = runnerSettings,
            executionContext = executionContext
        ).run(buildArgs)
    }
}
