/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCInteropRunner.Companion.run
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinNativeCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinNativeFragmentMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.isMainCompilationData
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.targets.native.internal.isAllowCommonizer
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.project.model.LanguageSettings
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.utils.ResolvedDependencies as KResolvedDependencies
import org.jetbrains.kotlin.utils.ResolvedDependenciesSupport as KResolvedDependenciesSupport
import org.jetbrains.kotlin.utils.ResolvedDependency as KResolvedDependency
import org.jetbrains.kotlin.utils.ResolvedDependencyArtifactPath as KResolvedDependencyArtifactPath
import org.jetbrains.kotlin.utils.ResolvedDependencyId as KResolvedDependencyId
import org.jetbrains.kotlin.utils.ResolvedDependencyVersion as KResolvedDependencyVersion

// TODO: It's just temporary tasks used while KN isn't integrated with Big Kotlin compilation infrastructure.
// region Useful extensions
internal fun MutableList<String>.addArg(parameter: String, value: String) {
    add(parameter)
    add(value)
}

internal fun MutableList<String>.addArgs(parameter: String, values: Iterable<String>) {
    values.forEach {
        addArg(parameter, it)
    }
}

internal fun MutableList<String>.addArgIfNotNull(parameter: String, value: String?) {
    if (value != null) {
        addArg(parameter, value)
    }
}

internal fun MutableList<String>.addKey(key: String, enabled: Boolean) {
    if (enabled) {
        add(key)
    }
}

internal fun MutableList<String>.addFileArgs(parameter: String, values: FileCollection) {
    values.files.forEach {
        addArg(parameter, it.canonicalPath)
    }
}

internal fun MutableList<String>.addFileArgs(parameter: String, values: Collection<FileCollection>) {
    values.forEach {
        addFileArgs(parameter, it)
    }
}

private fun File.providedByCompiler(project: Project): Boolean =
    toPath().startsWith(project.file(project.konanHome).resolve("klib").toPath())

// We need to filter out interop duplicates because we create copy of them for IDE.
// TODO: Remove this after interop rework.
private fun FileCollection.filterOutPublishableInteropLibs(project: Project): FileCollection {
    val libDirectories = project.rootProject.allprojects.map { it.buildDir.resolve("libs").absoluteFile.toPath() }
    return filter { file ->
        !(file.name.contains("-cinterop-") && libDirectories.any { file.toPath().startsWith(it) })
    }
}

/**
 * We pass to the compiler:
 *
 *    - Only *.klib files and directories (normally containing an unpacked klib).
 *      A dependency configuration may contain jar files
 *      (e.g. when a common artifact was directly added to commonMain source set).
 *      So, we need to filter out such artifacts.
 *
 *    - Only existing files. We don't compile a klib if there are no sources
 *      for it (NO-SOURCE check). So we need to take this case into account
 *      and skip libraries that were not compiled. See also: GH-2617 (K/N repo).
 */
private fun Collection<File>.filterKlibsPassedToCompiler(): List<File> = filter {
    (it.extension == "klib" || it.isDirectory) && it.exists()
}

// endregion
abstract class AbstractKotlinNativeCompile<T : KotlinCommonToolOptions, K : KotlinNativeCompilationData<*>> : AbstractCompile() {
    @get:Internal
    abstract val compilation: K

    // region inputs/outputs
    @get:Input
    abstract val outputKind: CompilerOutputKind

    @get:Input
    abstract val optimized: Boolean

    @get:Input
    abstract val debuggable: Boolean

    @get:Internal
    abstract val baseName: String

    @get:Internal
    protected val objects = project.objects

    @get:Internal
    protected val konanTarget by project.provider {
        compilation.konanTarget
    }

    // Inputs and outputs
    @IgnoreEmptyDirectories
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree {
        return super.getSource()
    }

    @get:Classpath
    val libraries: FileCollection by project.provider {
        // Avoid resolving these dependencies during task graph construction when we can't build the target:
        if (konanTarget.enabledOnCurrentHost)
            compilation.compileDependencyFiles.filterOutPublishableInteropLibs(project)
        else objects.fileCollection()
    }

    @get:Classpath
    protected val friendModule: FileCollection by project.provider {
        project.files(compilation.friendPaths)
    }

    @Deprecated("For native tasks use 'libraries' instead", ReplaceWith("libraries"))
    override fun getClasspath(): FileCollection = libraries
    override fun setClasspath(configuration: FileCollection?) {
        throw UnsupportedOperationException("Setting classpath directly is unsupported.")
    }

    @get:Input
    val target: String by project.provider { compilation.konanTarget.name }

    // region Compiler options.
    @get:Internal
    abstract val kotlinOptions: T
    abstract fun kotlinOptions(fn: T.() -> Unit)
    abstract fun kotlinOptions(fn: Closure<*>)

    @get:Input
    abstract val additionalCompilerOptions: Provider<Collection<String>>

    @get:Internal
    val languageSettings: LanguageSettings by project.provider {
        compilation.languageSettings
    }

    @get:Input
    val progressiveMode: Boolean
        get() = languageSettings.progressiveMode
    // endregion.

    @get:Input
    val enableEndorsedLibs: Boolean by project.provider { compilation.enableEndorsedLibs }

    @get:Input
    val kotlinNativeVersion: String
        get() = project.konanVersion.toString()

    @get:Input
    internal val useEmbeddableCompilerJar: Boolean
        get() = project.nativeUseEmbeddableCompilerJar

    @Internal
    open val outputFile: Provider<File> = project.provider {
        val prefix = outputKind.prefix(konanTarget)
        val suffix = outputKind.suffix(konanTarget)
        val filename = "$prefix${baseName}$suffix".let {
            when {
                outputKind == FRAMEWORK ->
                    it.asValidFrameworkName()
                outputKind in listOf(STATIC, DYNAMIC) || outputKind == PROGRAM && konanTarget == KonanTarget.WASM32 ->
                    it.replace('-', '_')
                else -> it
            }
        }

        destinationDir.resolve(filename)
    }

    // endregion
    @Internal
    val compilerPluginOptions = CompilerPluginOptions()

    @get:Input
    val compilerPluginCommandLine
        get() = compilerPluginOptions.arguments

    @Optional
    @Classpath
    var compilerPluginClasspath: FileCollection? = null

    /**
     * Plugin Data provided by [KpmCompilerPlugin]
     */
    @get:Optional
    @get:Nested
    var kotlinPluginData: Provider<KotlinCompilerPluginData>? = null

    // Used by IDE via reflection.
    @get:Internal
    val serializedCompilerArguments: List<String>
        get() = buildCommonArgs()

    // Used by IDE via reflection.
    @get:Internal
    val defaultSerializedCompilerArguments: List<String>
        get() = buildCommonArgs(true)

    private val languageSettingsBuilder by project.provider {
        compilation.languageSettings
    }

    // Args used by both the compiler and IDEA.
    private fun buildCommonArgs(defaultsOnly: Boolean = false): List<String> {
        val plugins = listOfNotNull(
            compilerPluginClasspath?.let { CompilerPluginData(it, compilerPluginOptions) },
            kotlinPluginData?.orNull?.let { CompilerPluginData(it.classpath, it.options) }
        )
        val opts = object : KotlinCommonToolOptions {
            override var allWarningsAsErrors = kotlinOptions.allWarningsAsErrors
            override var suppressWarnings = kotlinOptions.suppressWarnings
            override var verbose = kotlinOptions.verbose
            override var freeCompilerArgs = if (defaultsOnly) emptyList() else additionalCompilerOptions.get().toList()
        }

        return buildKotlinNativeCommonArgs(
            languageSettings,
            enableEndorsedLibs,
            opts,
            plugins
        )
    }

    @get:Input
    @get:Optional
    internal val konanTargetsForManifest: String by project.provider {
        @Suppress("CAST_NEVER_SUCCEEDS") // TODO: this warning looks very suspicious, as if the code never works as intended.
        (compilation as? KotlinSharedNativeCompilation)
            ?.konanTargets
            ?.joinToString(separator = " ") { it.visibleName }
            .orEmpty()
    }

    @get:Internal
    internal val manifestFile: Provider<File>
        get() = project.provider {
            val inputManifestFile = project.buildDir.resolve("tmp/$name/inputManifest")
            inputManifestFile
        }
}

/**
 * A task producing a klibrary from a compilation.
 */
@CacheableTask
open class KotlinNativeCompile
@Inject
constructor(
    @Internal
    @Transient  // can't be serialized for Gradle configuration cache
    final override val compilation: KotlinNativeCompilationData<*>
) : AbstractKotlinNativeCompile<KotlinCommonOptions, KotlinNativeCompilationData<*>>(),
    KotlinCompile<KotlinCommonOptions> {

    @get:Input
    override val outputKind = LIBRARY

    @get:Input
    override val optimized = false

    @get:Input
    override val debuggable = true

    @get:Internal
    override val baseName: String by project.provider {
        if (compilation.isMainCompilationData())
            project.name
        else "${project.name}_${compilation.compilationPurpose}"
    }

    // Store as an explicit provider in order to allow Gradle Instant Execution to capture the state
//    private val allSourceProvider = compilation.map { project.files(it.allSources).asFileTree }

    @get:Input
    val moduleName: String by project.provider {
        project.klibModuleName(baseName)
    }

    @get:OutputFile
    override val outputFile: Provider<File>
        get() = super.outputFile

    @get:Input
    val shortModuleName: String by project.provider { baseName }

    // Inputs and outputs.
    // region Sources.

    @get:Internal // these sources are normally a subset of `source` ones which are already tracked
    val commonSources: ConfigurableFileCollection = project.files()

//    private val commonSources: FileCollection by lazy {
//        // Already taken into account in getSources method.
//        project.files(compilation.map { it.commonSources }).asFileTree
//    }

    private val commonSourcesTree: FileTree
        get() = commonSources.asFileTree

    // endregion.

    // region Language settings imported from a SourceSet.
    val languageVersion: String?
        @Optional @Input get() = languageSettings.languageVersion

    val apiVersion: String?
        @Optional @Input get() = languageSettings.apiVersion

    val enabledLanguageFeatures: Set<String>
        @Input get() = languageSettings.enabledLanguageFeatures

    @Deprecated("Unsupported and will be removed in next major releases", replaceWith = ReplaceWith("optInAnnotationsInUse"))
    val experimentalAnnotationsInUse: Set<String>
        @Internal get() = languageSettings.experimentalAnnotationsInUse

    val optInAnnotationsInUse: Set<String>
        @Input get() = languageSettings.optInAnnotationsInUse
    // endregion.

    // region Kotlin options.
    override val kotlinOptions: KotlinCommonOptions by project.provider {
        compilation.kotlinOptions
    }

    @get:Input
    override val additionalCompilerOptions: Provider<Collection<String>> = project.provider {
        kotlinOptions.freeCompilerArgs + ((languageSettings as? DefaultLanguageSettingsBuilder)?.freeCompilerArgs ?: emptyList())
    }

    override fun kotlinOptions(fn: KotlinCommonOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    override fun kotlinOptions(fn: Closure<*>) {
        fn.delegate = kotlinOptions
        fn.call()
    }
    // endregion.

    @TaskAction
    fun compile() {
        val output = outputFile.get()
        output.parentFile.mkdirs()

        var sharedCompilationData: SharedCompilationData? = null
        if (compilation is KotlinNativeFragmentMetadataCompilationData) {
            val manifestFile: File = manifestFile.get()
            manifestFile.ensureParentDirsCreated()
            val properties = java.util.Properties()
            properties[KLIB_PROPERTY_NATIVE_TARGETS] = konanTargetsForManifest
            properties.saveToFile(org.jetbrains.kotlin.konan.file.File(manifestFile.toPath()))

            sharedCompilationData = SharedCompilationData(
                manifestFile,
                project.isAllowCommonizer()
            )
        }

        val localKotlinOptions = object : KotlinCommonToolOptions {
            override var allWarningsAsErrors = kotlinOptions.allWarningsAsErrors
            override var suppressWarnings = kotlinOptions.suppressWarnings
            override var verbose = kotlinOptions.verbose
            override var freeCompilerArgs = additionalCompilerOptions.get().toList()
        }

        val plugins = listOfNotNull(
            compilerPluginClasspath?.let { CompilerPluginData(it, compilerPluginOptions) },
            kotlinPluginData?.orNull?.let { CompilerPluginData(it.classpath, it.options) }
        )

        val buildArgs = buildKotlinNativeKlibCompilerArgs(
            output,
            optimized,
            debuggable,
            konanTarget,
            libraries.files.filterKlibsPassedToCompiler(),
            languageSettings,
            enableEndorsedLibs,
            localKotlinOptions,
            plugins,
            moduleName,
            shortModuleName,
            friendModule,
            sharedCompilationData,
            source,
            commonSourcesTree
        )

        KotlinNativeCompilerRunner(project).run(buildArgs)
    }
}

/**
 * A task producing a final binary from a compilation.
 */
@CacheableTask
open class KotlinNativeLink
@Inject
constructor(
    @Internal
    val binary: NativeBinary
) : AbstractKotlinNativeCompile<KotlinCommonToolOptions, KotlinNativeCompilation>() {
    @get:Internal
    final override val compilation: KotlinNativeCompilation
        get() = binary.compilation

    init {
        dependsOn(project.provider { compilation.compileKotlinTaskProvider })
        // Frameworks actively uses symlinks.
        // Gradle build cache transforms symlinks into regular files https://guides.gradle.org/using-build-cache/#symbolic_links
        outputs.cacheIf { outputKind != FRAMEWORK }
    }

    @Internal // Taken into account by getSources().
    val intermediateLibrary: Provider<File> = project.provider { compilation.compileKotlinTask.outputFile.get() }

    @IgnoreEmptyDirectories
    @InputFiles
    @SkipWhenEmpty
    override fun getSource(): FileTree =
        objects.fileCollection().from(intermediateLibrary).asFileTree

    @OutputDirectory
    override fun getDestinationDir(): File {
        return binary.outputDirectory
    }

    override fun setDestinationDir(destinationDir: File) {
        binary.outputDirectory = destinationDir
    }

    override val outputKind: CompilerOutputKind
        @Input get() = binary.outputKind.compilerOutputKind

    override val optimized: Boolean
        @Input get() = binary.optimized

    override val debuggable: Boolean
        @Input get() = binary.debuggable

    override val baseName: String
        @Input get() = binary.baseName

    @get:Input
    protected val konanCacheKind: NativeCacheKind by project.provider {
        project.getKonanCacheKind(konanTarget)
    }

    inner class NativeLinkOptions : KotlinCommonToolOptions {
        override var allWarningsAsErrors: Boolean = false
        override var suppressWarnings: Boolean = false
        override var verbose: Boolean = false
        override var freeCompilerArgs: List<String> = listOf()
    }

    // We propagate compilation free args to the link task for now (see KT-33717).
    @get:Input
    override val additionalCompilerOptions: Provider<Collection<String>> = project.provider {
        kotlinOptions.freeCompilerArgs +
                compilation.kotlinOptions.freeCompilerArgs +
                ((languageSettings as? DefaultLanguageSettingsBuilder)?.freeCompilerArgs ?: emptyList())
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

    val binaryOptions: Map<String, String>
        @Input get() = PropertiesProvider(project).nativeBinaryOptions + binary.binaryOptions

    val processTests: Boolean
        @Input get() = binary is TestExecutable

    @get:Classpath
    val exportLibraries: FileCollection by project.provider {
        binary.let {
            if (it is AbstractNativeLibrary) {
                project.configurations.getByName(it.exportConfigurationName)
            } else {
                objects.fileCollection()
            }
        }
    }

    @get:Input
    val isStaticFramework: Boolean by project.provider {
        binary.let { it is Framework && it.isStatic }
    }

    @get:Input
    val embedBitcode: BitcodeEmbeddingMode by project.provider {
        (binary as? Framework)?.embedBitcode ?: BitcodeEmbeddingMode.DISABLE
    }

    @get:Internal
    val apiFilesProvider = project.provider {
        project.configurations.getByName(compilation.apiConfigurationName).files.filterKlibsPassedToCompiler()
    }

    private fun validatedExportedLibraries() {
        val exportConfiguration = exportLibraries as? Configuration ?: return
        val apiFiles = apiFilesProvider.get()

        val failed = mutableSetOf<Dependency>()
        exportConfiguration.allDependencies.forEach {
            val dependencyFiles = exportConfiguration.files(it).filterKlibsPassedToCompiler()
            if (!apiFiles.containsAll(dependencyFiles)) {
                failed.add(it)
            }
        }

        check(failed.isEmpty()) {
            val failedDependenciesList = failed.joinToString(separator = "\n") {
                when (it) {
                    is FileCollectionDependency -> "|Files: ${it.files.files}"
                    is ProjectDependency -> "|Project ${it.dependencyProject.path}"
                    else -> "|${it.group}:${it.name}:${it.version}"
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

    @TaskAction
    fun compile() {
        validatedExportedLibraries()

        val output = outputFile.get()
        output.parentFile.mkdirs()

        val plugins = listOfNotNull(
            compilerPluginClasspath?.let { CompilerPluginData(it, compilerPluginOptions) },
            kotlinPluginData?.orNull?.let { CompilerPluginData(it.classpath, it.options) }
        )

        val localKotlinOptions = object : KotlinCommonToolOptions {
            override var allWarningsAsErrors = kotlinOptions.allWarningsAsErrors
            override var suppressWarnings = kotlinOptions.suppressWarnings
            override var verbose = kotlinOptions.verbose
            override var freeCompilerArgs = additionalCompilerOptions.get().toList()
        }

        val externalDependenciesArgs = ExternalDependenciesBuilder(project, compilation).buildCompilerArgs()
        val cacheArgs = CacheBuilder(project, binary, konanTarget, externalDependenciesArgs).buildCompilerArgs()

        val buildArgs = buildKotlinNativeBinaryLinkerArgs(
            output,
            optimized,
            debuggable,
            konanTarget,
            outputKind,
            libraries.files.filterKlibsPassedToCompiler(),
            friendModule,
            languageSettings,
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
            listOf(intermediateLibrary.get()),
            externalDependenciesArgs + cacheArgs
        )

        KotlinNativeCompilerRunner(project).run(buildArgs)
    }
}

private class ExternalDependenciesBuilder(
    val project: Project,
    val compilation: KotlinCompilation<*>,
    intermediateLibraryName: String?
) {
    constructor(project: Project, compilation: KotlinNativeCompilation) : this(
        project, compilation, compilation.compileKotlinTask.moduleName
    )

    private val compileDependencyConfiguration: Configuration
        get() = project.configurations.getByName(compilation.compileDependencyConfigurationName)

    private val sourceCodeModuleId: KResolvedDependencyId =
        intermediateLibraryName?.let { KResolvedDependencyId(it) } ?: KResolvedDependencyId.DEFAULT_SOURCE_CODE_MODULE_ID

    private val konanPropertiesService: KonanPropertiesBuildService
        get() = KonanPropertiesBuildService.registerIfAbsent(project.gradle).get()

    fun buildCompilerArgs(): List<String> {
        val compilerVersion = Distribution.getCompilerVersion(konanPropertiesService.compilerVersion, project.konanHome)
        val konanVersion = compilerVersion?.let(CompilerVersion.Companion::fromString)
            ?: project.konanVersion

        if (konanVersion.isAtLeast(1, 6, 0)) {
            val dependenciesFile = writeDependenciesFile(buildDependencies(), deleteOnExit = true)
            if (dependenciesFile != null)
                return listOf("-Xexternal-dependencies=${dependenciesFile.path}")
        }

        return emptyList()
    }

    private fun buildDependencies(): Collection<KResolvedDependency> {
        // Collect all artifacts.
        val moduleNameToArtifactPaths: MutableMap</* unique name*/ String, MutableSet<KResolvedDependencyArtifactPath>> = mutableMapOf()
        compileDependencyConfiguration.incoming.artifacts.artifacts.mapNotNull { resolvedArtifact ->
            val uniqueName = (resolvedArtifact.id.componentIdentifier as? ModuleComponentIdentifier)?.uniqueName ?: return@mapNotNull null
            val artifactPath = resolvedArtifact.file.absolutePath

            moduleNameToArtifactPaths.getOrPut(uniqueName) { mutableSetOf() } += KResolvedDependencyArtifactPath(artifactPath)
        }

        // The build system may express the single module as two modules where the first one is a common
        // module without artifacts and the second one is a platform-specific module with mandatory artifact.
        // Example: "org.jetbrains.kotlinx:atomicfu" (common) and "org.jetbrains.kotlinx:atomicfu-macosx64" (platform-specific).
        // Both such modules should be merged into a single module with just two names:
        // "org.jetbrains.kotlinx:atomicfu (org.jetbrains.kotlinx:atomicfu-macosx64)".
        val moduleIdsToMerge: MutableMap</* platform-specific */ KResolvedDependencyId, /* common */ KResolvedDependencyId> = mutableMapOf()

        // Collect plain modules.
        val plainModules: MutableMap<KResolvedDependencyId, KResolvedDependency> = mutableMapOf()
        fun processModule(resolvedDependency: DependencyResult, incomingDependencyId: KResolvedDependencyId) {
            if (resolvedDependency !is ResolvedDependencyResult) return

            val requestedModule = resolvedDependency.requested as? ModuleComponentSelector ?: return
            val selectedModule = resolvedDependency.selected
            val selectedModuleId = selectedModule.id as? ModuleComponentIdentifier ?: return

            val moduleId = KResolvedDependencyId(selectedModuleId.uniqueName)
            val module = plainModules.getOrPut(moduleId) {
                val artifactPaths = moduleId.uniqueNames.asSequence()
                    .mapNotNull { uniqueName -> moduleNameToArtifactPaths[uniqueName] }
                    .firstOrNull()
                    .orEmpty()

                KResolvedDependency(
                    id = moduleId,
                    selectedVersion = KResolvedDependencyVersion(selectedModuleId.version),
                    requestedVersionsByIncomingDependencies = mutableMapOf(), // To be filled in just below.
                    artifactPaths = artifactPaths.toMutableSet()
                )
            }

            // Record the requested version of the module by the current incoming dependency.
            module.requestedVersionsByIncomingDependencies[incomingDependencyId] = KResolvedDependencyVersion(requestedModule.version)

            // TODO: Use [ResolvedDependencyResult.resolvedVariant.externalVariant] to find a connection between platform-specific
            //  and common modules when "resolvedVariant" and "externalVariant" graduate from incubating state.
            if (module.artifactPaths.isNotEmpty()) {
                val originModuleId = resolvedDependency.from.id as? ModuleComponentIdentifier
                if (originModuleId != null
                    && selectedModuleId.group == originModuleId.group
                    && selectedModuleId.module.startsWith(originModuleId.module)
                    && selectedModuleId.version == originModuleId.version
                ) {
                    // These two modules should be merged.
                    moduleIdsToMerge[moduleId] = KResolvedDependencyId(originModuleId.uniqueName)
                }
            }

            selectedModule.dependencies.forEach { processModule(it, incomingDependencyId = moduleId) }
        }

        compileDependencyConfiguration.incoming.resolutionResult.root.dependencies.forEach { dependencyResult ->
            processModule(dependencyResult, incomingDependencyId = sourceCodeModuleId)
        }

        if (moduleIdsToMerge.isEmpty())
            return plainModules.values

        // Do merge.
        val replacedModules: MutableMap</* old module ID */ KResolvedDependencyId, /* new module */ KResolvedDependency> = mutableMapOf()
        moduleIdsToMerge.forEach { (platformSpecificModuleId, commonModuleId) ->
            val platformSpecificModule = plainModules.getValue(platformSpecificModuleId)
            val commonModule = plainModules.getValue(commonModuleId)

            val replacementModuleId = KResolvedDependencyId(platformSpecificModuleId.uniqueNames + commonModuleId.uniqueNames)
            val replacementModule = KResolvedDependency(
                id = replacementModuleId,
                visibleAsFirstLevelDependency = commonModule.visibleAsFirstLevelDependency,
                selectedVersion = commonModule.selectedVersion,
                requestedVersionsByIncomingDependencies = mutableMapOf<KResolvedDependencyId, KResolvedDependencyVersion>().apply {
                    this += commonModule.requestedVersionsByIncomingDependencies
                    this += platformSpecificModule.requestedVersionsByIncomingDependencies - commonModuleId
                },
                artifactPaths = mutableSetOf<KResolvedDependencyArtifactPath>().apply {
                    this += commonModule.artifactPaths
                    this += platformSpecificModule.artifactPaths
                }
            )

            replacedModules[platformSpecificModuleId] = replacementModule
            replacedModules[commonModuleId] = replacementModule
        }

        // Assemble new modules together (without "replaced" and with "replacements").
        val mergedModules: MutableMap<KResolvedDependencyId, KResolvedDependency> = mutableMapOf()
        mergedModules += plainModules - replacedModules.keys
        replacedModules.values.forEach { replacementModule -> mergedModules[replacementModule.id] = replacementModule }

        // Fix references to point to "replacement" modules instead of "replaced" modules.
        mergedModules.values.forEach { module ->
            module.requestedVersionsByIncomingDependencies.mapNotNull { (replacedModuleId, requestedVersion) ->
                val replacementModuleId = replacedModules[replacedModuleId]?.id ?: return@mapNotNull null
                Triple(replacedModuleId, replacementModuleId, requestedVersion)
            }.forEach { (replacedModuleId, replacementModuleId, requestedVersion) ->
                module.requestedVersionsByIncomingDependencies.remove(replacedModuleId)
                module.requestedVersionsByIncomingDependencies[replacementModuleId] = requestedVersion
            }
        }

        return mergedModules.values
    }

    private fun writeDependenciesFile(dependencies: Collection<KResolvedDependency>, deleteOnExit: Boolean): File? {
        if (dependencies.isEmpty()) return null

        val dependenciesFile = Files.createTempFile("kotlin-native-external-dependencies", ".deps").toAbsolutePath().toFile()
        if (deleteOnExit) dependenciesFile.deleteOnExit()
        dependenciesFile.writeText(KResolvedDependenciesSupport.serialize(KResolvedDependencies(dependencies, sourceCodeModuleId)))
        return dependenciesFile
    }

    private val ModuleComponentIdentifier.uniqueName: String
        get() = "$group:$module"

    companion object {
        @Suppress("unused") // Used for tests only. Accessed via reflection.
        @JvmStatic
        fun buildExternalDependenciesFileForTests(project: Project): File? {
            val compilation = project.tasks.asSequence()
                .filterIsInstance<KotlinNativeLink>()
                .map { it.binary }
                .filterIsInstance<Executable>() // Not TestExecutable or any other kind of NativeBinary. Strictly Executable!
                .firstOrNull()
                ?.compilation
                ?: return null

            return with(ExternalDependenciesBuilder(project, compilation)) {
                val dependencies = buildDependencies().sortedBy { it.id.toString() }
                writeDependenciesFile(dependencies, deleteOnExit = false)
            }
        }
    }
}

internal class CacheBuilder(
    val project: Project,
    val binary: NativeBinary,
    val konanTarget: KonanTarget,
    val externalDependenciesArgs: List<String>
) {

    private val nativeSingleFileResolveStrategy: SingleFileKlibResolveStrategy
        get() = CompilerSingleFileKlibResolveAllowingIrProvidersStrategy(
            listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
        )
    private val compilation: KotlinNativeCompilation
        get() = binary.compilation

    private val optimized: Boolean
        get() = binary.optimized

    private val debuggable: Boolean
        get() = binary.debuggable

    private val konanPropertiesService: KonanPropertiesBuildService
        get() = KonanPropertiesBuildService.registerIfAbsent(project.gradle).get()

    private val konanCacheKind: NativeCacheKind
        get() = project.getKonanCacheKind(konanTarget)

    // Inputs and outputs
    private val libraries: FileCollection
        get() = compilation.compileDependencyFiles.filterOutPublishableInteropLibs(project)

    private val target: String
        get() = konanTarget.name

    private val rootCacheDirectory by project.provider {
        getRootCacheDirectory(File(project.konanHome), konanTarget, debuggable, konanCacheKind)
    }

    private fun getCacheDirectory(
        dependency: ResolvedDependency
    ): File {
        return getCacheDirectory(rootCacheDirectory, dependency)
    }

    private fun needCache(libraryPath: String) =
        libraryPath.startsWith(project.gradle.gradleUserHomeDir.absolutePath) && libraryPath.endsWith(".klib")

    private fun ensureDependencyPrecached(dependency: ResolvedDependency, visitedDependencies: MutableSet<ResolvedDependency>) {
        if (dependency in visitedDependencies)
            return
        visitedDependencies += dependency
        dependency.children.forEach { ensureDependencyPrecached(it, visitedDependencies) }

        val artifactsToAddToCache = dependency.moduleArtifacts.filter { needCache(it.file.absolutePath) }
        if (artifactsToAddToCache.isEmpty()) return

        val dependenciesCacheDirectories = getDependenciesCacheDirectories(
            rootCacheDirectory,
            dependency
        ) ?: return

        val cacheDirectory = getCacheDirectory(dependency)
        cacheDirectory.mkdirs()

        val artifactsLibraries = artifactsToAddToCache
            .map {
                resolveSingleFileKlib(
                    KFile(it.file.absolutePath),
                    logger = GradleLoggerAdapter(project.logger),
                    strategy = nativeSingleFileResolveStrategy
                )
            }
            .associateBy { it.uniqueName }

        // Top sort artifacts.
        val sortedLibraries = mutableListOf<KotlinLibrary>()
        val visitedLibraries = mutableSetOf<KotlinLibrary>()

        fun dfs(library: KotlinLibrary) {
            visitedLibraries += library
            library.unresolvedDependencies
                .map { artifactsLibraries[it.path] }
                .forEach {
                    if (it != null && it !in visitedLibraries)
                        dfs(it)
                }
            sortedLibraries += library
        }

        for (library in artifactsLibraries.values)
            if (library !in visitedLibraries)
                dfs(library)

        for (library in sortedLibraries) {
            if (File(cacheDirectory, library.uniqueName.cachedName).listFilesOrEmpty().isNotEmpty())
                continue
            project.logger.info("Compiling ${library.uniqueName} to cache")
            val args = mutableListOf(
                "-p", konanCacheKind.produce!!,
                "-target", target
            )
            if (debuggable)
                args += "-g"
            args += konanPropertiesService.additionalCacheFlags(konanTarget)
            args += externalDependenciesArgs
            args += "-Xadd-cache=${library.libraryFile.absolutePath}"
            args += "-Xcache-directory=${cacheDirectory.absolutePath}"
            args += "-Xcache-directory=${rootCacheDirectory.absolutePath}"

            dependenciesCacheDirectories.forEach {
                args += "-Xcache-directory=${it.absolutePath}"
            }
            getAllDependencies(dependency)
                .flatMap { it.moduleArtifacts }
                .map { it.file }
                .filterKlibsPassedToCompiler()
                .forEach {
                    args += "-l"
                    args += it.absolutePath
                }
            library.unresolvedDependencies
                .mapNotNull { artifactsLibraries[it.path] }
                .forEach {
                    args += "-l"
                    args += it.libraryFile.absolutePath
                }
            KotlinNativeCompilerRunner(project).run(args)
        }
    }

    private val String.cachedName
        get() = getCacheFileName(this, konanCacheKind)

    private fun ensureCompilerProvidedLibPrecached(
        platformLibName: String,
        platformLibs: Map<String, File>,
        visitedLibs: MutableSet<String>
    ) {
        if (platformLibName in visitedLibs)
            return
        visitedLibs += platformLibName
        val platformLib = platformLibs[platformLibName] ?: error("$platformLibName is not found in platform libs")
        if (File(rootCacheDirectory, platformLibName.cachedName).listFilesOrEmpty().isNotEmpty())
            return
        val unresolvedDependencies = resolveSingleFileKlib(
            KFile(platformLib.absolutePath),
            logger = GradleLoggerAdapter(project.logger),
            strategy = nativeSingleFileResolveStrategy
        ).unresolvedDependencies
        for (dependency in unresolvedDependencies)
            ensureCompilerProvidedLibPrecached(dependency.path, platformLibs, visitedLibs)
        project.logger.info("Compiling $platformLibName (${visitedLibs.size}/${platformLibs.size}) to cache")
        val args = mutableListOf(
            "-p", konanCacheKind.produce!!,
            "-target", target
        )
        if (debuggable)
            args += "-g"
        // It's a dirty workaround, but we need a Gradle Build Service for a proper solution,
        // which is too big to put in 1.6.0, so let's use ad-hoc solution for now.
        // TODO: https://youtrack.jetbrains.com/issue/KT-48553.
        if (konanTarget == KonanTarget.IOS_ARM64) {
            // See https://youtrack.jetbrains.com/issue/KT-48552
            args += "-Xembed-bitcode-marker"
        }
        args += "-Xadd-cache=${platformLib.absolutePath}"
        args += "-Xcache-directory=${rootCacheDirectory.absolutePath}"
        KotlinNativeCompilerRunner(project).run(args)
    }

    private fun ensureCompilerProvidedLibsPrecached() {
        val distribution = Distribution(project.konanHome)
        val platformLibs = mutableListOf<File>().apply {
            this += File(distribution.stdlib)
            this += File(distribution.platformLibs(konanTarget)).listFiles().orEmpty()
        }.associateBy { it.name }
        val visitedLibs = mutableSetOf<String>()
        for (platformLibName in platformLibs.keys)
            ensureCompilerProvidedLibPrecached(platformLibName, platformLibs, visitedLibs)
    }

    fun buildCompilerArgs(): List<String> = mutableListOf<String>().apply {
        if (konanCacheKind != NativeCacheKind.NONE && !optimized && konanPropertiesService.cacheWorksFor(konanTarget)) {
            rootCacheDirectory.mkdirs()
            ensureCompilerProvidedLibsPrecached()
            add("-Xcache-directory=${rootCacheDirectory.absolutePath}")
            val visitedDependencies = mutableSetOf<ResolvedDependency>()
            val allCacheDirectories = mutableSetOf<String>()
            val compileDependencyConfiguration = project.configurations.getByName(compilation.compileDependencyConfigurationName)
            for (root in compileDependencyConfiguration.resolvedConfiguration.firstLevelModuleDependencies) {
                ensureDependencyPrecached(root, visitedDependencies)
                for (dependency in listOf(root) + getAllDependencies(root)) {
                    val cacheDirectory = getCacheDirectory(dependency)
                    if (cacheDirectory.exists())
                        allCacheDirectories += cacheDirectory.absolutePath
                }
            }
            for (cacheDirectory in allCacheDirectories)
                add("-Xcache-directory=$cacheDirectory")
        }
    }

    companion object {
        internal fun getRootCacheDirectory(konanHome: File, target: KonanTarget, debuggable: Boolean, cacheKind: NativeCacheKind): File {
            require(cacheKind != NativeCacheKind.NONE) { "Usupported cache kind: ${NativeCacheKind.NONE}" }
            val optionsAwareCacheName = "$target${if (debuggable) "-g" else ""}$cacheKind"
            return konanHome.resolve("klib/cache/$optionsAwareCacheName")
        }

        internal fun getCacheFileName(baseName: String, cacheKind: NativeCacheKind): String =
            cacheKind.outputKind?.let {
                "${baseName}-cache"
            } ?: error("No output for kind $cacheKind")
    }
}

@CacheableTask
open class CInteropProcess @Inject constructor(@get:Internal val settings: DefaultCInteropSettings) : DefaultTask() {

    @Internal // Taken into account in the outputFileProvider property
    lateinit var destinationDir: Provider<File>

    val konanTarget: KonanTarget
        @Input get() = settings.compilation.konanTarget

    val interopName: String
        @Input get() = settings.name

    val baseKlibName: String
        @Input get() {
            val compilationPrefix = settings.compilation.let {
                if (it.isMainCompilationData()) project.name else it.compilationPurpose
            }
            return "$compilationPrefix-cinterop-$interopName"
        }

    val outputFileName: String
        @Internal get() = with(CompilerOutputKind.LIBRARY) {
            "$baseKlibName${suffix(konanTarget)}"
        }

    val moduleName: String
        @Input get() = project.klibModuleName(baseKlibName)

    @get:Internal
    val outputFile: File
        get() = outputFileProvider.get()

    init {
        outputs.upToDateWhen { outputFile.exists() }
    }

    // Inputs and outputs.

    @OutputFile
    val outputFileProvider: Provider<File> = project.provider { destinationDir.get().resolve(outputFileName) }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val defFile: File
        get() = settings.defFileProperty.get()

    val packageName: String?
        @Optional @Input get() = settings.packageName

    val compilerOpts: List<String>
        @Input get() = settings.compilerOpts

    val linkerOpts: List<String>
        @Input get() = settings.linkerOpts

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val headers: FileCollection
        get() = settings.headers

    val allHeadersDirs: Set<File>
        @Input get() = settings.includeDirs.allHeadersDirs.files

    val headerFilterDirs: Set<File>
        @Input get() = settings.includeDirs.headerFilterDirs.files

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val libraries: FileCollection
        get() = settings.dependencyFiles.filterOutPublishableInteropLibs(project)

    val extraOpts: List<String>
        @Input get() = settings.extraOpts

    val kotlinNativeVersion: String
        @Input get() = project.konanVersion.toString()

    // Task action.
    @TaskAction
    fun processInterop() {
        val args = mutableListOf<String>().apply {
            addArg("-o", outputFile.absolutePath)

            addArgIfNotNull("-target", konanTarget.visibleName)
            addArgIfNotNull("-def", defFile.canonicalPath)
            addArgIfNotNull("-pkg", packageName)

            addFileArgs("-header", headers)

            compilerOpts.forEach {
                addArg("-compiler-option", it)
            }

            linkerOpts.forEach {
                addArg("-linker-option", it)
            }

            libraries.files.filterKlibsPassedToCompiler().forEach { library ->
                addArg("-library", library.absolutePath)
            }

            addArgs("-compiler-option", allHeadersDirs.map { "-I${it.absolutePath}" })
            addArgs("-headerFilterAdditionalSearchPrefix", headerFilterDirs.map { it.absolutePath })

            if (project.konanVersion.isAtLeast(1, 4, 0)) {
                addArg("-Xmodule-name", moduleName)
            }

            addAll(extraOpts)
        }

        outputFile.parentFile.mkdirs()
        KotlinNativeCInteropRunner.createExecutionContext(this).run(args)
    }
}
