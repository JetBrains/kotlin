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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCompilerRunner
import org.jetbrains.kotlin.compilerRunner.KotlinToolRunner
import org.jetbrains.kotlin.compilerRunner.getKonanCacheKind
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.targets.native.tasks.CompilerPluginData
import org.jetbrains.kotlin.gradle.targets.native.tasks.buildKotlinNativeBinaryLinkerArgs
import org.jetbrains.kotlin.gradle.utils.ResolvedDependencyGraph
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
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
    private val providerFactory: ProviderFactory,
    private val execOperations: ExecOperations
) : AbstractKotlinNativeCompile<KotlinCommonToolOptions, KotlinNativeCompilation, StubK2NativeCompilerArguments>(objectFactory) {
    @get:Internal
    final override val compilation: KotlinNativeCompilation
        get() = binary.compilation

    private val runnerSettings = KotlinNativeCompilerRunner.Settings.fromProject(project)

    init {
        dependsOn(project.provider { compilation.compileKotlinTaskProvider })
        // Frameworks actively uses symlinks.
        // Gradle build cache transforms symlinks into regular files https://guides.gradle.org/using-build-cache/#symbolic_links
        outputs.cacheIf { outputKind != CompilerOutputKind.FRAMEWORK }

        this.setSource(compilation.compileKotlinTask.outputFile)
        includes.clear() // we need to include non '.kt' or '.kts' files
        disallowSourceChanges()
    }

    override val destinationDirectory: DirectoryProperty = binary.outputDirectoryProperty

    override val outputKind: CompilerOutputKind
        @Input get() = binary.outputKind.compilerOutputKind

    override val optimized: Boolean
        @Input get() = binary.optimized

    override val debuggable: Boolean
        @Input get() = binary.debuggable

    override val baseName: String
        @Input get() = binary.baseName

    @get:Input
    protected val konanCacheKind: NativeCacheKind by lazy {
        project.getKonanCacheKind(konanTarget)
    }

    inner class NativeLinkOptions : KotlinCommonToolOptions {
        override var allWarningsAsErrors: Boolean = false
        override var suppressWarnings: Boolean = false
        override var verbose: Boolean = false
        override var freeCompilerArgs: List<String> = listOf()
    }

    private val nativeLinkArgs = PropertiesProvider(project).nativeLinkArgs

    // We propagate compilation free args to the link task for now (see KT-33717).
    @get:Input
    override val additionalCompilerOptions: Provider<Collection<String>> = providerFactory.provider {
        kotlinOptions.freeCompilerArgs +
                compilation.kotlinOptions.freeCompilerArgs +
                ((languageSettings as? DefaultLanguageSettingsBuilder)?.freeCompilerArgs ?: emptyList()) +
                nativeLinkArgs
    }

    override val kotlinOptions: KotlinCommonToolOptions = NativeLinkOptions()

    override fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    override fun kotlinOptions(fn: Closure<*>) {
        fn.delegate = kotlinOptions
        fn.call()
    }

    // Binary-specific options.
    val entryPoint: String?
        @Input
        @Optional
        get() = (binary as? Executable)?.entryPoint

    val linkerOpts: List<String>
        @Input get() = binary.linkerOpts

    @get:Input
    val binaryOptions: Map<String, String> by lazy { PropertiesProvider(project).nativeBinaryOptions + binary.binaryOptions }

    val processTests: Boolean
        @Input get() = binary is TestExecutable

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

    @get:Input
    val embedBitcode: BitcodeEmbeddingMode
        get() = (binary as? Framework)?.embedBitcode ?: BitcodeEmbeddingMode.DISABLE

    @get:Internal
    val apiFiles = project.files(project.configurations.getByName(compilation.apiConfigurationName)).filterKlibsPassedToCompiler()

    private val localKotlinOptions get() =
        object : KotlinCommonToolOptions {
            override var allWarningsAsErrors = kotlinOptions.allWarningsAsErrors
            override var suppressWarnings = kotlinOptions.suppressWarnings
            override var verbose = kotlinOptions.verbose
            override var freeCompilerArgs = additionalCompilerOptions.get().toList()
        }

    private val externalDependenciesArgs by lazy { ExternalDependenciesBuilder(project, compilation).buildCompilerArgs() }

    private val cacheBuilderSettings by lazy {
        CacheBuilder.Settings.createWithProject(project, binary, konanTarget, localKotlinOptions, externalDependenciesArgs)
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

    private val resolvedDependencyGraph = ResolvedDependencyGraph(
        project.configurations.getByName(compilation.compileDependencyConfigurationName)
    )

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

        val buildArgs = buildKotlinNativeBinaryLinkerArgs(
            output,
            optimized,
            debuggable,
            konanTarget,
            outputKind,
            libraries.files.filterKlibsPassedToCompiler(),
            friendModule.files.toList(),
            enableEndorsedLibs,
            localKotlinOptions,
            plugins,
            processTests,
            entryPoint,
            embedBitcode,
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
