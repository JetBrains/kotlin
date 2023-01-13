package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.EagerInitializedPropertySerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer
import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.LinkerOutputKind
import org.jetbrains.kotlin.konan.target.presetName

internal fun determineLinkerOutput(context: PhaseContext): LinkerOutputKind =
        when (context.config.produce) {
            CompilerOutputKind.FRAMEWORK -> {
                val staticFramework = context.config.produceStaticFramework
                if (staticFramework) LinkerOutputKind.STATIC_LIBRARY else LinkerOutputKind.DYNAMIC_LIBRARY
            }
            CompilerOutputKind.DYNAMIC_CACHE,
            CompilerOutputKind.DYNAMIC -> LinkerOutputKind.DYNAMIC_LIBRARY
            CompilerOutputKind.STATIC_CACHE,
            CompilerOutputKind.STATIC -> LinkerOutputKind.STATIC_LIBRARY
            CompilerOutputKind.PROGRAM -> run {
                if (context.config.target.family == Family.ANDROID) {
                    val configuration = context.config.configuration
                    val androidProgramType = configuration.get(BinaryOptions.androidProgramType) ?: AndroidProgramType.Default
                    if (androidProgramType.linkerOutputKindOverride != null) {
                        return@run androidProgramType.linkerOutputKindOverride
                    }
                }
                LinkerOutputKind.EXECUTABLE
            }
            else -> TODO("${context.config.produce} should not reach native linker stage")
        }

internal class CacheStorage(val generationState: NativeGenerationState) {
    private val config = generationState.config
    private val outputFiles = generationState.outputFiles

    fun renameOutput() {
        // For caches the output file is a directory. It might be created by someone else,
        // we have to delete it in order for the next renaming operation to succeed.
        // TODO: what if the directory is not empty?
        java.io.File(outputFiles.mainFileName).delete()
        if (!outputFiles.tempCacheDirectory!!.renameTo(outputFiles.mainFile))
            outputFiles.tempCacheDirectory.deleteRecursively()
    }

    fun saveAdditionalCacheInfo() {
        outputFiles.prepareTempDirectories()
        saveCacheBitcodeDependencies()
        saveInlineFunctionBodies()
        saveClassFields()
        saveEagerInitializedProperties()
    }

    private fun saveCacheBitcodeDependencies() {
        outputFiles.bitcodeDependenciesFile!!.writeLines(
                DependenciesSerializer.serialize(generationState.dependenciesTracker.immediateBitcodeDependencies))
    }

    private fun saveInlineFunctionBodies() {
        outputFiles.inlineFunctionBodiesFile!!.writeBytes(
                InlineFunctionBodyReferenceSerializer.serialize(generationState.inlineFunctionBodies))
    }

    private fun saveClassFields() {
        outputFiles.classFieldsFile!!.writeBytes(
                ClassFieldsSerializer.serialize(generationState.classFields))
    }

    private fun saveEagerInitializedProperties() {
        outputFiles.eagerInitializedPropertiesFile!!.writeBytes(
                EagerInitializedPropertySerializer.serialize(generationState.eagerInitializedFiles))
    }
}

// TODO: We have a Linker.kt file in the shared module.
internal class Linker(val generationState: NativeGenerationState) {
    private val config = generationState.config
    private val platform = config.platform
    private val linkerOutput = determineLinkerOutput(generationState)
    private val linker = platform.linker
    private val target = config.target
    private val optimize = generationState.shouldOptimize()
    private val debug = config.debug || config.lightDebug

    fun link(objectFiles: List<ObjectFile>) {
        val nativeDependencies = generationState.dependenciesTracker.nativeDependenciesToLink

        val includedBinariesLibraries = config.libraryToCache?.let { listOf(it.klib) }
                ?: nativeDependencies.filterNot { config.cachedLibraries.isLibraryCached(it) }
        val includedBinaries = includedBinariesLibraries.map { (it as? KonanLibrary)?.includedPaths.orEmpty() }.flatten()

        val libraryProvidedLinkerFlags = generationState.dependenciesTracker.allNativeDependencies.map { it.linkerOpts }.flatten()

        runLinker(objectFiles, includedBinaries, libraryProvidedLinkerFlags)
    }

    private fun asLinkerArgs(args: List<String>): List<String> {
        if (linker.useCompilerDriverAsLinker) {
            return args
        }

        val result = mutableListOf<String>()
        for (arg in args) {
            // If user passes compiler arguments to us - transform them to linker ones.
            if (arg.startsWith("-Wl,")) {
                result.addAll(arg.substring(4).split(','))
            } else {
                result.add(arg)
            }
        }
        return result
    }

    private fun runLinker(objectFiles: List<ObjectFile>,
                          includedBinaries: List<String>,
                          libraryProvidedLinkerFlags: List<String>): ExecutableFile? {
        val outputFiles = generationState.outputFiles

        val additionalLinkerArgs: List<String>
        val executable: String

        if (config.produce != CompilerOutputKind.FRAMEWORK) {
            additionalLinkerArgs = if (target.family.isAppleFamily) {
                when (config.produce) {
                    CompilerOutputKind.DYNAMIC_CACHE ->
                        listOf("-install_name", outputFiles.dynamicCacheInstallName)
                    else -> listOf("-dead_strip")
                }
            } else {
                emptyList()
            }
            executable = outputFiles.nativeBinaryFile
        } else {
            val framework = File(generationState.outputFile)
            val dylibName = framework.name.removeSuffix(".framework")
            val dylibRelativePath = when (target.family) {
                Family.IOS,
                Family.TVOS,
                Family.WATCHOS -> dylibName
                Family.OSX -> "Versions/A/$dylibName"
                else -> error(target)
            }
            additionalLinkerArgs = listOf("-dead_strip", "-install_name", "@rpath/${framework.name}/$dylibRelativePath")
            val dylibPath = framework.child(dylibRelativePath)
            dylibPath.parentFile.mkdirs()
            executable = dylibPath.absolutePath
        }

        val needsProfileLibrary = generationState.coverage.enabled
        val mimallocEnabled = config.allocationMode == AllocationMode.MIMALLOC

        val linkerInput = determineLinkerInput(objectFiles, linkerOutput)
        try {
            File(executable).delete()
            val linkerArgs = asLinkerArgs(config.configuration.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                    BitcodeEmbedding.getLinkerOptions(config) +
                    linkerInput.caches.dynamic +
                    libraryProvidedLinkerFlags + additionalLinkerArgs

            val finalOutputCommands = linker.finalLinkCommands(
                    objectFiles = linkerInput.objectFiles,
                    executable = executable,
                    libraries = linker.linkStaticLibraries(includedBinaries) + linkerInput.caches.static,
                    linkerArgs = linkerArgs,
                    optimize = optimize,
                    debug = debug,
                    kind = linkerOutput,
                    outputDsymBundle = outputFiles.symbolicInfoFile,
                    needsProfileLibrary = needsProfileLibrary,
                    mimallocEnabled = mimallocEnabled,
                    sanitizer = config.sanitizer
            )
            (linkerInput.preLinkCommands + finalOutputCommands).forEach {
                it.logWith(generationState::log)
                it.execute()
            }
        } catch (e: KonanExternalToolFailure) {
            val extraUserInfo =
                    if (linkerInput.cachingInvolved)
                        """
                        Please try to disable compiler caches and rerun the build. To disable compiler caches, add the following line to the gradle.properties file in the project's root directory:
                            
                            kotlin.native.cacheKind.${target.presetName}=none
                            
                        Also, consider filing an issue with full Gradle log here: https://kotl.in/issue
                        """.trimIndent()
                    else ""
            generationState.reportCompilationError("${e.toolName} invocation reported errors\n$extraUserInfo\n${e.message}")
        }
        return executable
    }

    private fun shouldPerformPreLink(caches: CachesToLink, linkerOutputKind: LinkerOutputKind): Boolean {
        // Pre-link is only useful when producing static library. Otherwise its just a waste of time.
        val isStaticLibrary = linkerOutputKind == LinkerOutputKind.STATIC_LIBRARY &&
                config.isFinalBinary
        val enabled = config.cacheSupport.preLinkCaches
        val nonEmptyCaches = caches.static.isNotEmpty()
        return isStaticLibrary && enabled && nonEmptyCaches
    }

    private fun determineLinkerInput(objectFiles: List<ObjectFile>, linkerOutputKind: LinkerOutputKind): LinkerInput {
        val caches = determineCachesToLink(generationState)
        // Since we have several linker stages that involve caching,
        // we should detect cache usage early to report errors correctly.
        val cachingInvolved = caches.static.isNotEmpty() || caches.dynamic.isNotEmpty()
        return when {
            config.produce == CompilerOutputKind.STATIC_CACHE -> {
                // Do not link static cache dependencies.
                LinkerInput(objectFiles, CachesToLink(emptyList(), caches.dynamic), emptyList(), cachingInvolved)
            }
            shouldPerformPreLink(caches, linkerOutputKind) -> {
                val preLinkResult = generationState.tempFiles.create("withStaticCaches", ".o").absolutePath
                val preLinkCommands = linker.preLinkCommands(objectFiles + caches.static, preLinkResult)
                LinkerInput(listOf(preLinkResult), CachesToLink(emptyList(), caches.dynamic), preLinkCommands, cachingInvolved)
            }
            else -> LinkerInput(objectFiles, caches, emptyList(), cachingInvolved)
        }
    }
}

private class LinkerInput(
        val objectFiles: List<ObjectFile>,
        val caches: CachesToLink,
        val preLinkCommands: List<Command>,
        val cachingInvolved: Boolean
)

private class CachesToLink(val static: List<String>, val dynamic: List<String>)

private fun determineCachesToLink(generationState: NativeGenerationState): CachesToLink {
    val staticCaches = mutableListOf<String>()
    val dynamicCaches = mutableListOf<String>()

    generationState.dependenciesTracker.allCachedBitcodeDependencies.forEach { dependency ->
        val library = dependency.library
        val currentBinaryContainsLibrary = generationState.llvmModuleSpecification.containsLibrary(library)
        val cache = generationState.config.cachedLibraries.getLibraryCache(library)
                ?: error("Library $library is expected to be cached")

        // Consistency check. Generally guaranteed by implementation.
        if (currentBinaryContainsLibrary)
            error("Library ${library.libraryName} is found in both cache and current binary")

        val list = when (cache.kind) {
            CachedLibraries.Kind.DYNAMIC -> dynamicCaches
            CachedLibraries.Kind.STATIC -> staticCaches
        }

        list += if (dependency.kind is DependenciesTracker.DependencyKind.CertainFiles && cache is CachedLibraries.Cache.PerFile)
            dependency.kind.files.map { cache.getFileBinaryPath(it) }
        else cache.binariesPaths
    }
    return CachesToLink(static = staticCaches, dynamic = dynamicCaches)
}
