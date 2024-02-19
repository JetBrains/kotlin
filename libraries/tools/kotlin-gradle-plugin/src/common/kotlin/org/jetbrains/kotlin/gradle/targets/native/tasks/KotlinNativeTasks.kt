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
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCInteropRunner.Companion.run
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.create
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.statistics.NativeCompilerOptionMetrics
import org.jetbrains.kotlin.gradle.plugin.statistics.UsesBuildFusService
import org.jetbrains.kotlin.gradle.report.*
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.UsesKotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.GradleLoggerAdapter
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.project.model.LanguageSettings
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import kotlin.collections.associateBy
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.isNotEmpty
import kotlin.collections.orEmpty
import kotlin.collections.plus
import kotlin.collections.plusAssign
import kotlin.collections.set
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

internal fun MutableList<String>.addFileArgs(parameter: String, values: FileCollection) {
    values.files.forEach {
        addArg(parameter, it.canonicalPath)
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
private val File.canKlibBePassedToCompiler get() = (extension == "klib" || isDirectory) && exists()

internal fun Collection<File>.filterKlibsPassedToCompiler(): List<File> = filter(File::canKlibBePassedToCompiler)

/* Returned FileCollection is lazy */
internal fun FileCollection.filterKlibsPassedToCompiler(): FileCollection = filter(File::canKlibBePassedToCompiler)

// endregion
@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AbstractKotlinNativeCompile<
        T : KotlinCommonToolOptions,
        M : CommonToolArguments,
        >
@Inject constructor(
    private val objectFactory: ObjectFactory,
) : AbstractKotlinCompileTool<M>(objectFactory) {

    @get:Inject
    protected abstract val projectLayout: ProjectLayout

    @get:Internal
    internal abstract val compilation: KotlinCompilationInfo

    // region inputs/outputs
    @get:Input
    abstract val outputKind: CompilerOutputKind

    @get:Input
    abstract val optimized: Boolean

    @get:Input
    abstract val debuggable: Boolean

    @get:Internal
    abstract val baseName: String

    @get:Input
    internal val produceUnpackedKlib: Property<Boolean> = objectFactory.propertyWithConvention(false)

    @get:Input
    @get:Optional
    internal abstract val explicitApiMode: Property<ExplicitApiMode>

    @get:Internal
    internal val konanTarget by project.provider {
        when (val compilation = compilation) {
            is KotlinCompilationInfo.TCS -> (compilation.compilation as AbstractKotlinNativeCompilation).konanTarget
        }
    }


    @get:Classpath
    override val libraries: ConfigurableFileCollection = objectFactory.fileCollection().from(
        {
            // Avoid resolving these dependencies during task graph construction when we can't build the target:
            if (konanTarget.enabledOnCurrentHost)
                objectFactory.fileCollection().from({ compilation.compileDependencyFiles })
            else objectFactory.fileCollection()
        }
    )

    @get:Classpath
    internal val friendModule: FileCollection = project.files({ compilation.friendPaths })

    @get:Classpath
    internal val refinesModule: FileCollection = project.files({ compilation.refinesPaths })

    @get:Input
    val target: String by project.provider { konanTarget.name }

    // region Compiler options.
    @Deprecated(
        message = "AbstractKotlinNativeCompile will not provide access to kotlinOptions." +
                " Implementations should provide access to compilerOptions",
    )
    @get:Internal
    abstract val kotlinOptions: T

    @Deprecated(
        message = "AbstractKotlinNativeCompile will not provide access to kotlinOptions()." +
                " Implementations should provide access to compilerOptions()",
    )
    abstract fun kotlinOptions(fn: T.() -> Unit)

    @Deprecated(
        message = "AbstractKotlinNativeCompile will not provide access to kotlinOptions()." +
                " Implementations should provide access to compilerOptions()",
    )
    abstract fun kotlinOptions(fn: Closure<*>)

    @Deprecated("Use implementations compilerOptions to get/set freeCompilerArgs")
    @get:Input
    abstract val additionalCompilerOptions: Provider<Collection<String>>

    @Deprecated("Use implementations compilerOptions")
    @get:Internal
    val languageSettings: LanguageSettings
        get() = compilation.languageSettings

    @Suppress("DeprecatedCallableAddReplaceWith")
    @get:Deprecated("Replaced with 'compilerOptions.progressiveMode'")
    @get:Internal
    val progressiveMode: Boolean
        get() = compilation.compilerOptions.options.progressiveMode.get()
    // endregion.

    @get:Input
    val kotlinNativeVersion: String = project.konanVersion

    @get:Input
    val artifactVersion = project.version.toString()

    @get:Input
    internal val useEmbeddableCompilerJar: Boolean = project.nativeUseEmbeddableCompilerJar

    @get:Internal
    open val outputFile: Provider<File>
        get() = destinationDirectory.flatMap {
            val prefix = outputKind.prefix(konanTarget)
            val suffix = if (produceUnpackedKlib.get()) "" else outputKind.suffix(konanTarget)
            val filename = "$prefix${baseName}$suffix".let {
                when {
                    outputKind == FRAMEWORK ->
                        it.asValidFrameworkName()

                    outputKind in listOf(STATIC, DYNAMIC) || outputKind == PROGRAM && konanTarget == KonanTarget.WASM32 ->
                        it.replace('-', '_')

                    else -> it
                }
            }

            objectFactory.property(it.file(filename).asFile)
        }

    // endregion
    @Internal
    val compilerPluginOptions = CompilerPluginOptions()

    @get:Input
    val compilerPluginCommandLine
        get() = compilerPluginOptions.arguments

    @Optional
    @Classpath
    open var compilerPluginClasspath: FileCollection? = null

    /**
     * Plugin Data provided by [KpmCompilerPlugin]
     */
    @get:Optional
    @get:Nested
    var kotlinPluginData: Provider<KotlinCompilerPluginData>? = null

    private val languageSettingsBuilder by project.provider {
        compilation.languageSettings
    }

    @get:Internal
    internal val manifestFile: Provider<RegularFile> get() = projectLayout.buildDirectory.file("tmp/$name/inputManifest")

}

/**
 * A task producing a klibrary from a compilation.
 */
@CacheableTask
abstract class KotlinNativeCompile
@Inject
internal constructor(
    @get:Internal
    @Transient  // can't be serialized for Gradle configuration cache
    final override val compilation: KotlinCompilationInfo,
    override val compilerOptions: KotlinNativeCompilerOptions,
    private val objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory,
    private val execOperations: ExecOperations,
) : AbstractKotlinNativeCompile<KotlinCommonOptions, K2NativeCompilerArguments>(objectFactory),
    KotlinCompile<KotlinCommonOptions>,
    K2MultiplatformCompilationTask,
    UsesBuildMetricsService,
    KotlinCompilationTask<KotlinNativeCompilerOptions>,
    UsesBuildFusService,
    UsesKotlinNativeBundleBuildService {

    @get:Input
    override val outputKind = LIBRARY

    @get:Input
    override val optimized = false

    @get:Input
    override val debuggable = true

    @get:Internal
    override val baseName: String by lazy {
        if (compilation.isMain) project.name
        else "${project.name}_${compilation.compilationName}"
    }

    // Store as an explicit provider in order to allow Gradle Instant Execution to capture the state
//    private val allSourceProvider = compilation.map { project.files(it.allSources).asFileTree }

    @Deprecated(
        message = "Please use 'compilerOptions.moduleName' to configure",
        replaceWith = ReplaceWith("compilerOptions.moduleName.get()")
    )
    @get:Internal
    val moduleName: String get() = compilerOptions.moduleName.get()

    @get:Input
    val shortModuleName: String by providerFactory.provider { baseName }

    // Inputs and outputs.
    // region Sources.

    @get:Internal // these sources are normally a subset of `source` ones which are already tracked
    val commonSources: ConfigurableFileCollection = project.files()

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

    @get:Nested
    override val multiplatformStructure: K2MultiplatformStructure = objectFactory.newInstance()

    private val commonSourcesTree: FileTree
        get() = commonSources.asFileTree

    // endregion.

    // region Language settings imported from a SourceSet.
    @Deprecated(
        message = "Replaced with kotlinOptions.languageVersion",
        replaceWith = ReplaceWith("kotlinOptions.languageVersion")
    )
    val languageVersion: String?
        @Optional @Input get() = compilerOptions.languageVersion.orNull?.version

    @Deprecated(
        message = "Replaced with kotlinOptions.apiVersion",
        replaceWith = ReplaceWith("kotlinOptions.apiVersion")
    )
    val apiVersion: String?
        @Optional @Input get() = compilerOptions.apiVersion.orNull?.version

    @Deprecated("Language features is internal Kotlin compiler flags and should not be used directly")
    val enabledLanguageFeatures: Set<String>
        @Internal get() = compilerOptions
            .freeCompilerArgs.get()
            .filter { it.startsWith("-XXLanguage:+") }
            .toSet()

    @Deprecated(
        message = "Replaced with compilerOptions.optIn",
        replaceWith = ReplaceWith("compilerOptions.optIn")
    )
    val optInAnnotationsInUse: Set<String>
        @Internal get() = compilerOptions.optIn.get().toSet()
    // endregion.

    // region Kotlin options

    override val kotlinOptions: KotlinCommonOptions = object : KotlinCommonOptions {
        override val options: KotlinCommonCompilerOptions
            get() = compilerOptions
    }

    override fun kotlinOptions(fn: KotlinCommonOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    @Deprecated(
        message = "Replaced with kotlinOptions()",
        replaceWith = ReplaceWith("kotlinOptions(fn)")
    )
    override fun kotlinOptions(fn: Closure<*>) {
        @Suppress("DEPRECATION")
        fn.delegate = kotlinOptions
        fn.call()
    }

    @Suppress("UNCHECKED_CAST")
    @Deprecated(
        message = "Replaced with compilerOptions.freeCompilerArgs",
        replaceWith = ReplaceWith("compilerOptions.freeCompilerArgs.get()")
    )
    @get:Input
    override val additionalCompilerOptions: Provider<Collection<String>>
        get() = compilerOptions.freeCompilerArgs as Provider<Collection<String>>

    private val runnerSettings = KotlinNativeCompilerRunner.Settings.of(
        kotlinNativeProvider.get().bundleDirectory.getFile().absolutePath,
        kotlinNativeProvider.get().konanDataDir.orNull,
        project
    )
    // endregion.

    /**
     * This is utility property that contains list of native platform dependencies that are present in [compileDependencyFiles]
     * but should be excluded from actual classpath because they are included by default by Kotlin Native Compiler.
     * this behaviour will be fixed as part of KT-65232
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal var excludeOriginalPlatformLibraries: FileCollection? = null

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("KTIJ-25227: Necessary override for IDEs < 2023.2", level = DeprecationLevel.ERROR)
    override fun setupCompilerArgs(args: K2NativeCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        @Suppress("DEPRECATION_ERROR")
        super.setupCompilerArgs(args, defaultsOnly, ignoreClasspathResolutionErrors)
    }

    override fun createCompilerArguments(context: CreateCompilerArgumentsContext) = context.create<K2NativeCompilerArguments> {
        val sharedCompilationData = createSharedCompilationDataOrNull()

        val compilerPlugins = listOfNotNull(
            compilerPluginClasspath?.let { CompilerPluginData(it, compilerPluginOptions) },
            kotlinPluginData?.orNull?.let { CompilerPluginData(it.classpath, it.options) }
        )

        primitive { args ->
            args.moduleName = compilerOptions.moduleName.get()
            args.shortModuleName = shortModuleName
            args.multiPlatform = true
            args.noendorsedlibs = true
            args.outputName = outputFile.get().absolutePath
            args.optimization = optimized
            args.debug = debuggable
            args.enableAssertions = debuggable
            args.target = konanTarget.name
            args.produce = outputKind.name.toLowerCaseAsciiOnly()
            args.metadataKlib = sharedCompilationData != null
            args.nodefaultlibs = sharedCompilationData != null
            args.nostdlib = true
            args.manifestFile = sharedCompilationData?.manifestFile?.absolutePath
            args.nopack = produceUnpackedKlib.get()

            args.pluginOptions = compilerPlugins.flatMap { it.options.arguments }.toTypedArray()

            /* Shared native compilations in K2 still use -Xcommon-sources and klib dependencies */
            if (compilerOptions.usesK2.get() && sharedCompilationData == null) {
                args.fragments = multiplatformStructure.fragmentsCompilerArgs
                args.fragmentRefines = multiplatformStructure.fragmentRefinesCompilerArgs
            }

            KotlinNativeCompilerOptionsHelper.fillCompilerArguments(compilerOptions, args)

            explicitApiMode.orNull?.run { args.explicitApi = toCompilerValue() }
        }

        pluginClasspath { args ->
            args.pluginClasspaths = compilerPlugins.flatMap { classpath -> runSafe { classpath.files } ?: emptySet() }.toPathsArray()
        }

        dependencyClasspath { args ->
            args.libraries = runSafe {
                libraries.exclude(excludeOriginalPlatformLibraries).files.filterKlibsPassedToCompiler().toPathsArray()
            }
            args.friendModules = runSafe {
                friendModule.files.takeIf { it.isNotEmpty() }?.map { it.absolutePath }?.joinToString(File.pathSeparator)
            }
            args.refinesPaths = runSafe {
                sharedCompilationData?.refinesPaths?.files?.takeIf { it.isNotEmpty() }?.toPathsArray()
            }
        }

        sources { args ->
            /* Shared native compilations in K2 still use -Xcommon-sources and klib dependencies */
            if (compilerOptions.usesK2.get() && sharedCompilationData == null) {
                args.fragmentSources = multiplatformStructure.fragmentSourcesCompilerArgs(sources.files, sourceFileFilter)
            } else {
                args.commonSources = commonSourcesTree.files.takeIf { it.isNotEmpty() }?.toPathsArray()
            }

            args.freeArgs += sources.asFileTree.map { it.absolutePath }
        }
    }

    @get:Internal
    internal val isMetadataCompilation: Boolean = when (compilation) {
        is KotlinCompilationInfo.TCS -> compilation.compilation is KotlinMetadataCompilation<*>
    }

    private fun createSharedCompilationDataOrNull(): SharedCompilationData? {
        if (!isMetadataCompilation) return null

        val manifestFile: File = manifestFile.get().asFile
        manifestFile.ensureParentDirsCreated()
        val properties = java.util.Properties()
        /**
         * We're overwriting the native_targets field in the klib manifest, because otherwise it will contain
         * only one passed `-target` or host-target, if nothing is passed. Both options are wrong for shared-native
         * compilations.
         *
         * If the native_targets manifest will contain too few targets, IDE and compilation will not work properly (e.g., it will forbid
         * dependencies on such metadata-klibs from shared native source sets, because it will consider that a source set with targets
         * like (iosArm64, iosSimulatorArm64) can't depend on a klib with native_targets=iosArm64)
         *
         * Now, overwriting it to empty value is entirely unintended behavior that appeared long time ago (~Kotlin 1.4) due to some
         * unfortunate merges. Surprisingly, it gives the desired behavior.
         *
         * While we could change this code to contain the proper set of KonanTargets (the ones that are actually the targets of shared-native
         * compilation), that would introduce one new "flavour" of manifests/binaries, which complicates a bit further work in this area.
         *
         * So, for now we're leaving this "hack" as is. Refer to KT-64525 "Clean-up target-related fields in manifest of klibs" for the
         * plan on proper fixes
         */
        properties[KLIB_PROPERTY_NATIVE_TARGETS] = ""
        properties.saveToFile(org.jetbrains.kotlin.konan.file.File(manifestFile.toPath()))

        return SharedCompilationData(manifestFile, refinesModule)
    }

    @TaskAction
    fun compile() {
        val buildMetrics = metrics.get()
        addBuildMetricsForTaskAction(
            metricsReporter = buildMetrics,
            languageVersion = resolveLanguageVersion()
        ) {
            val arguments = createCompilerArguments()
            val buildArguments = buildMetrics.measure(GradleBuildTime.OUT_OF_WORKER_TASK_ACTION) {
                val output = outputFile.get()
                output.parentFile.mkdirs()

                buildFusService.orNull?.reportFusMetrics {
                    NativeCompilerOptionMetrics.collectMetrics(compilerOptions, it)
                }

                ArgumentUtils.convertArgumentsToStringList(arguments)
            }

            KotlinNativeCompilerRunner(
                settings = runnerSettings,
                executionContext = KotlinToolRunner.GradleExecutionContext.fromTaskContext(objectFactory, execOperations, logger),
                metricsReporter = buildMetrics
            ).run(buildArguments)
        }

    }

    private fun resolveLanguageVersion() =
        compilerOptions.languageVersion.orNull?.version?.let { v -> KotlinVersion.fromVersion(v) }

}

internal class ExternalDependenciesBuilder(
    val project: Project,
    val compilation: KotlinCompilation<*>,
    intermediateLibraryName: String?,
) {
    constructor(project: Project, compilation: KotlinNativeCompilation) : this(
        project, compilation, compilation.compileTaskProvider.get().compilerOptions.moduleName.get()
    )

    private val compileDependencyConfiguration: Configuration
        get() = project.configurations.getByName(compilation.compileDependencyConfigurationName)

    private val sourceCodeModuleId: KResolvedDependencyId =
        intermediateLibraryName?.let { KResolvedDependencyId(it) } ?: KResolvedDependencyId.DEFAULT_SOURCE_CODE_MODULE_ID

    private val konanPropertiesService: KonanPropertiesBuildService
        get() = KonanPropertiesBuildService.registerIfAbsent(project).get()

    fun buildCompilerArgs(): List<String> {
        val dependenciesFile = writeDependenciesFile(buildDependencies(), deleteOnExit = true)
        return if (dependenciesFile != null)
            listOf("-Xexternal-dependencies=${dependenciesFile.path}")
        else
            emptyList()
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
        val processedDependencies = hashSetOf<ResolvedDependencyResult>()
        fun processModule(resolvedDependency: DependencyResult, incomingDependencyId: KResolvedDependencyId) {
            if (resolvedDependency !is ResolvedDependencyResult) return
            if (resolvedDependency.isConstraint) return
            if (!processedDependencies.add(resolvedDependency)) return

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
    private val executionContext: KotlinToolRunner.GradleExecutionContext,
    private val settings: Settings,
    private val konanPropertiesService: KonanPropertiesBuildService,
    private val metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
) {
    class Settings(
        val runnerSettings: KotlinNativeCompilerRunner.Settings,
        val konanCacheKind: NativeCacheKind,
        val libraries: FileCollection,
        val gradleUserHomeDir: File,
        val konanTarget: KonanTarget,
        val toolOptions: KotlinCommonCompilerToolOptions,
        val externalDependenciesArgs: List<String>,
        val debuggable: Boolean,
        val optimized: Boolean,
    ) {
        val rootCacheDirectory
            get() = getRootCacheDirectory(
                File(runnerSettings.parent.konanHome),
                konanTarget,
                debuggable,
                konanCacheKind
            )

        companion object {
            fun createWithProject(
                konanHome: String,
                konanDataDir: String?,
                project: Project,
                binary: NativeBinary,
                konanTarget: KonanTarget,
                toolOptions: KotlinCommonCompilerToolOptions,
                externalDependenciesArgs: List<String>,
            ): Settings {
                val konanCacheKind = project.getKonanCacheKind(konanTarget)
                return Settings(
                    runnerSettings = KotlinNativeCompilerRunner.Settings.of(konanHome, konanDataDir, project),
                    konanCacheKind = konanCacheKind,
                    libraries = binary.compilation.compileDependencyFiles,
                    gradleUserHomeDir = project.gradle.gradleUserHomeDir,
                    konanTarget = konanTarget,
                    toolOptions = toolOptions,
                    externalDependenciesArgs = externalDependenciesArgs,
                    debuggable = binary.debuggable,
                    optimized = binary.optimized,
                )
            }
        }
    }


    private val nativeSingleFileResolveStrategy: SingleFileKlibResolveStrategy
        get() = CompilerSingleFileKlibResolveAllowingIrProvidersStrategy(
            listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
        )

    private val konanTarget: KonanTarget
        get() = settings.konanTarget

    private val optimized: Boolean
        get() = settings.optimized

    private val debuggable: Boolean
        get() = settings.debuggable

    private val konanCacheKind: NativeCacheKind
        get() = settings.konanCacheKind

    // Inputs and outputs
    private val libraries: FileCollection
        get() = settings.libraries

    private val target: String
        get() = konanTarget.name

    private val rootCacheDirectory: File
        get() = settings.rootCacheDirectory

    private val partialLinkageMode: String
        get() = settings.toolOptions.freeCompilerArgs.get().mapNotNull { arg ->
            arg.substringAfter("$PARTIAL_LINKAGE_PARAMETER=", missingDelimiterValue = "").takeIf(String::isNotEmpty)
        }.lastOrNull() ?: PartialLinkageMode.DEFAULT.name

    private fun getCacheDirectory(
        resolvedConfiguration: LazyResolvedConfiguration,
        dependency: ResolvedDependencyResult,
    ): File = getCacheDirectory(
        rootCacheDirectory = rootCacheDirectory,
        dependency = dependency,
        artifact = null,
        resolvedConfiguration = resolvedConfiguration,
        partialLinkageMode = partialLinkageMode
    )

    private fun needCache(libraryPath: String) =
        libraryPath.startsWith(settings.gradleUserHomeDir.absolutePath) && libraryPath.endsWith(".klib")

    private fun LazyResolvedConfiguration.ensureDependencyPrecached(
        dependency: ResolvedDependencyResult,
        visitedDependencies: MutableSet<ResolvedDependencyResult>,
    ) {
        if (dependency in visitedDependencies)
            return

        visitedDependencies += dependency
        dependency
            .selected
            .dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .forEach { ensureDependencyPrecached(it, visitedDependencies) }

        val artifactsToAddToCache = getArtifacts(dependency).filter { needCache(it.file.absolutePath) }

        if (artifactsToAddToCache.isEmpty()) return

        val dependenciesCacheDirectories = getDependenciesCacheDirectories(
            rootCacheDirectory = rootCacheDirectory,
            dependency = dependency,
            considerArtifact = false,
            resolvedConfiguration = this,
            partialLinkageMode = partialLinkageMode
        ) ?: return

        val cacheDirectory = getCacheDirectory(this, dependency)
        cacheDirectory.mkdirs()

        val artifactsLibraries = artifactsToAddToCache
            .map {
                resolveSingleFileKlib(
                    KFile(it.file.absolutePath),
                    logger = GradleLoggerAdapter(executionContext.logger),
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
            executionContext.logger.info("Compiling ${library.uniqueName} to cache")
            val args = mutableListOf(
                "-p", konanCacheKind.produce!!,
                "-target", target
            )
            if (debuggable) args += "-g"
            args += konanPropertiesService.additionalCacheFlags(konanTarget)
            args += settings.externalDependenciesArgs
            args += "$PARTIAL_LINKAGE_PARAMETER=$partialLinkageMode"
            args += "-Xadd-cache=${library.libraryFile.absolutePath}"
            args += "-Xcache-directory=${cacheDirectory.absolutePath}"
            args += "-Xcache-directory=${rootCacheDirectory.absolutePath}"

            dependenciesCacheDirectories.forEach {
                args += "-Xcache-directory=${it.absolutePath}"
            }
            getAllDependencies(dependency)
                .flatMap { getArtifacts(it) }
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
            KotlinNativeCompilerRunner(settings.runnerSettings, executionContext, GradleBuildMetricsReporter()).run(args)
        }
    }

    private val String.cachedName
        get() = getCacheFileName(this, konanCacheKind)

    private fun ensureCompilerProvidedLibPrecached(
        platformLibName: String,
        platformLibs: Map<String, File>,
        visitedLibs: MutableSet<String>,
    ) {
        if (platformLibName in visitedLibs)
            return
        visitedLibs += platformLibName
        val platformLib = platformLibs[platformLibName] ?: error("$platformLibName is not found in platform libs")
        if (File(rootCacheDirectory, platformLibName.cachedName).listFilesOrEmpty().isNotEmpty())
            return
        val unresolvedDependencies = resolveSingleFileKlib(
            KFile(platformLib.absolutePath),
            logger = GradleLoggerAdapter(executionContext.logger),
            strategy = nativeSingleFileResolveStrategy
        ).unresolvedDependencies
        for (dependency in unresolvedDependencies)
            ensureCompilerProvidedLibPrecached(dependency.path, platformLibs, visitedLibs)
        executionContext.logger.info("Compiling $platformLibName (${visitedLibs.size}/${platformLibs.size}) to cache")
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
        KotlinNativeCompilerRunner(settings.runnerSettings, executionContext, metricsReporter).run(args)
    }

    private fun ensureCompilerProvidedLibsPrecached() {
        val distribution =
            Distribution(settings.runnerSettings.parent.konanHome, konanDataDir = settings.runnerSettings.parent.konanDataDir)
        val platformLibs = mutableListOf<File>().apply {
            this += File(distribution.stdlib)
            this += File(distribution.platformLibs(konanTarget)).listFiles().orEmpty()
        }.associateBy { it.name }
        val visitedLibs = mutableSetOf<String>()
        for (platformLibName in platformLibs.keys)
            ensureCompilerProvidedLibPrecached(platformLibName, platformLibs, visitedLibs)
    }

    fun buildCompilerArgs(resolvedConfiguration: LazyResolvedConfiguration): List<String> = mutableListOf<String>().apply {
        if (konanCacheKind != NativeCacheKind.NONE && !optimized && konanPropertiesService.cacheWorksFor(konanTarget)) {
            rootCacheDirectory.mkdirs()
            ensureCompilerProvidedLibsPrecached()
            add("-Xcache-directory=${rootCacheDirectory.absolutePath}")
            val visitedDependencies = mutableSetOf<ResolvedDependencyResult>()
            val allCacheDirectories = mutableSetOf<String>()
            for (root in resolvedConfiguration.root.dependencies.filterIsInstance<ResolvedDependencyResult>()) {
                resolvedConfiguration.ensureDependencyPrecached(root, visitedDependencies)
                for (dependency in listOf(root) + getAllDependencies(root)) {
                    val cacheDirectory = getCacheDirectory(resolvedConfiguration, dependency)
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
            require(cacheKind != NativeCacheKind.NONE) { "Unsupported cache kind: ${NativeCacheKind.NONE}" }
            val optionsAwareCacheName = "$target${if (debuggable) "-g" else ""}$cacheKind"
            return konanHome.resolve("klib/cache/$optionsAwareCacheName")
        }

        internal fun getCacheFileName(baseName: String, cacheKind: NativeCacheKind): String =
            cacheKind.outputKind?.let {
                "${baseName}-cache"
            } ?: error("No output for kind $cacheKind")

        private const val PARTIAL_LINKAGE_PARAMETER = "-Xpartial-linkage"
    }
}

@CacheableTask
abstract class CInteropProcess @Inject internal constructor(params: Params) :
    DefaultTask(), UsesBuildMetricsService, UsesKotlinNativeBundleBuildService {

    internal class Params(
        val settings: DefaultCInteropSettings,
        val targetName: String,
        val compilationName: String,
        val konanTarget: KonanTarget,
        val baseKlibName: String,
        val services: Services,
    ) {
        internal open class Services @Inject constructor(
            val objectFactory: ObjectFactory,
            val execOperations: ExecOperations,
        )
    }

    private val objectFactory: ObjectFactory = params.services.objectFactory

    private val execOperations: ExecOperations = params.services.execOperations

    @get:Internal
    internal val targetName: String = params.targetName

    @get:Internal
    internal val compilationName: String = params.compilationName

    @Internal
    val settings: DefaultCInteropSettings = params.settings

    @Internal // Taken into account in the outputFileProvider property
    lateinit var destinationDir: Provider<File>

    @get:Input
    val konanTarget: KonanTarget = params.konanTarget

    @get:Input
    val konanVersion: String = project.konanVersion

    @Suppress("unused")
    @get:Input
    val libraryVersion = project.version.toString()

    @Suppress("unused")
    @get:Input
    val interopName: String = params.settings.name

    @get:Input
    val baseKlibName: String = params.baseKlibName


    @get:Internal
    val outputFileName: String = with(LIBRARY) {
        "$baseKlibName${suffix(konanTarget)}"
    }

    @get:Input
    val moduleName: String = project.klibModuleName(baseKlibName)

    @Deprecated(
        "Eager outputFile was replaced with lazy outputFileProvider",
        replaceWith = ReplaceWith("outputFileProvider")
    )
    @get:Internal
    val outputFile: File
        get() = outputFileProvider.get()

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

    private val runnerSettings = KotlinNativeToolRunner.Settings.of(
        kotlinNativeProvider.get().bundleDirectory.getFile().absolutePath,
        kotlinNativeProvider.get().konanDataDir.orNull,
        project
    )
    // Inputs and outputs.

    @OutputFile
    val outputFileProvider: Provider<File> = project.provider { destinationDir.get().resolve(outputFileName) }

    //Error file will be written only for errors during a project sync because for the sync task mustn't fail
    //see: org.jetbrains.kotlin.gradle.targets.native.tasks.IdeaSyncKotlinNativeCInteropRunnerExecutionContext
    @get:OutputFile
    internal val errorFileProvider: Provider<File> = project.provider { destinationDir.get().resolve("cinterop_error.out") }

    init {
        //KTIJ-25563:
        //Failed CInterop task is successful if it was run during import to have properly imported project.
        //But successful task is up-to-date for next invocations.
        //We have to check up-to-date-ness only if CInterop didn't generate an error file
        outputs.upToDateWhen { !errorFileProvider.get().exists() }
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    @get:Optional
    abstract val definitionFile: RegularFileProperty

    @get:Internal
    @Deprecated(
        "This eager parameter is deprecated.",
        replaceWith = ReplaceWith("definitionFile")
    )
    val defFile: File get() = definitionFile.asFile.get()


    @get:Optional
    @get:Input
    val packageName: String? get() = settings.packageName

    @get:Input
    val compilerOpts: List<String> get() = settings.compilerOpts

    @get:Input
    val linkerOpts: List<String> get() = settings.linkerOpts

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val headers: FileCollection get() = settings.headers

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val allHeadersDirs: Set<File> get() = settings.includeDirs.allHeadersDirs.files

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val headerFilterDirs: Set<File> get() = settings.includeDirs.headerFilterDirs.files

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val libraries: FileCollection get() = settings.dependencyFiles

    @get:Input
    val extraOpts: List<String> get() = settings.extraOpts

    @get:Internal
    val metrics: Property<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>> = project.objects
        .property(GradleBuildMetricsReporter())

    private val isInIdeaSync = project.isInIdeaSync

    // Task action.
    @TaskAction
    fun processInterop() {
        val buildMetrics = metrics.get()

        val args =
            mutableListOf<String>().apply {
                addArg("-o", outputFileProvider.get().absolutePath)

                addArgIfNotNull("-target", konanTarget.visibleName)
                if (definitionFile.isPresent) {
                    addArgIfNotNull("-def", definitionFile.getFile().canonicalPath)
                }
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
                addArg("-Xmodule-name", moduleName)

                // TODO: Uncomment when KT-61096 is implemented.
                // Don't pass library version to C-interop tool. This leads to issues like this: KT-62515
                //addArg("-libraryVersion", libraryVersion)

                addAll(extraOpts)

            }
        addBuildMetricsForTaskAction(buildMetrics, languageVersion = null) {
            outputFileProvider.get().parentFile.mkdirs()
            KotlinNativeCInteropRunner.createExecutionContext(
                task = this,
                isInIdeaSync = isInIdeaSync,
                runnerSettings = runnerSettings,
                gradleExecutionContext = KotlinToolRunner.GradleExecutionContext.fromTaskContext(objectFactory, execOperations, logger),
                metricsReporter = buildMetrics
            ).run(args)
        }
    }

}
