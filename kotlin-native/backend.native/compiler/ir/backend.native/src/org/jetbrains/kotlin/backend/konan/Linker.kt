package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.konan.llvm.LlvmImports
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.backend.konan.phases.ErrorReportingContext
import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassFields
import org.jetbrains.kotlin.backend.konan.serialization.SerializedInlineFunctionReference
import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.LinkerOutputKind
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal fun determineLinkerOutput(config: KonanConfig): LinkerOutputKind =
        when (config.produce) {
            CompilerOutputKind.FRAMEWORK -> {
                val staticFramework = config.produceStaticFramework
                if (staticFramework) LinkerOutputKind.STATIC_LIBRARY else LinkerOutputKind.DYNAMIC_LIBRARY
            }

            CompilerOutputKind.DYNAMIC_CACHE,
            CompilerOutputKind.DYNAMIC -> LinkerOutputKind.DYNAMIC_LIBRARY

            CompilerOutputKind.STATIC_CACHE,
            CompilerOutputKind.STATIC -> LinkerOutputKind.STATIC_LIBRARY

            CompilerOutputKind.PROGRAM -> run {
                if (config.target.family == Family.ANDROID) {
                    val configuration = config.configuration
                    val androidProgramType = configuration.get(BinaryOptions.androidProgramType) ?: AndroidProgramType.Default
                    if (androidProgramType.linkerOutputKindOverride != null) {
                        return@run androidProgramType.linkerOutputKindOverride
                    }
                }
                LinkerOutputKind.EXECUTABLE
            }

            else -> TODO("${config.produce} should not reach native linker stage")
        }

internal class CacheStorage(
        private val config: KonanConfig,
        private val llvmImports: LlvmImports,
        private val inlineFunctionBodies: List<SerializedInlineFunctionReference>,
        private val classFields: List<SerializedClassFields>,
) {
    private val isPreliminaryCache = config.produce == CompilerOutputKind.PRELIMINARY_CACHE

    fun renameOutput() {
        val outputFiles = config.outputFiles
        // For caches the output file is a directory. It might be created by someone else,
        // we have to delete it in order for the next renaming operation to succeed.
        outputFiles.mainFile.delete()
        if (!outputFiles.tempCacheDirectory!!.renameTo(outputFiles.mainFile))
            outputFiles.tempCacheDirectory.deleteRecursively()
    }

    fun saveAdditionalCacheInfo() {
        config.outputFiles.tempCacheDirectory!!.mkdirs()
        if (!isPreliminaryCache)
            saveCacheBitcodeDependencies()
        if (isPreliminaryCache || config.configuration.get(KonanConfigKeys.FILE_TO_CACHE) == null) {
            saveInlineFunctionBodies()
            saveClassFields()
        }
    }

    private fun saveCacheBitcodeDependencies() {
        val bitcodeDependencies = config.resolvedLibraries
                .getFullList(TopologicalLibraryOrder)
                .filter {
                    require(it is KonanLibrary)
                    llvmImports.bitcodeIsUsed(it)
                            && it != config.cacheSupport.libraryToCache?.klib // Skip loops.
                }.cast<List<KonanLibrary>>()
        config.outputFiles.bitcodeDependenciesFile!!.writeLines(bitcodeDependencies.map { it.uniqueName })
    }

    private fun saveInlineFunctionBodies() {
        config.outputFiles.inlineFunctionBodiesFile!!.writeBytes(
                InlineFunctionBodyReferenceSerializer.serialize(inlineFunctionBodies))
    }

    private fun saveClassFields() {
        config.outputFiles.classFieldsFile!!.writeBytes(
                ClassFieldsSerializer.serialize(classFields))
    }
}

/**
 * Parts of [org.jetbrains.kotlin.backend.konan.llvm.Llvm] that are required at linker stage.
 */
class NecessaryLlvmParts(
        val allCachedBitcodeDependencies: List<KonanLibrary>,
        val nativeDependenciesToLink: List<KonanLibrary>,
        val allNativeDependencies: List<KonanLibrary>,
)

// TODO: We have a Linker.kt file in the shared module.
internal class Linker(
        private val llvm: NecessaryLlvmParts,
        private val llvmModuleSpecification: LlvmModuleSpecification,
        private val coverage: CoverageManager,
        private val config: KonanConfig,
        private val logger: LoggingContext,
        private val errorReporter: ErrorReportingContext,
) {

    private val checks = config.checks
    private val platform = config.platform
    private val configuration = config.configuration
    private val linkerOutput = determineLinkerOutput(config)
    private val linker = platform.linker
    private val target = config.target
    private val optimize = checks.shouldOptimize()
    private val debug = checks.shouldContainLocationDebugInfo()

    fun link(objectFiles: List<ObjectFile>) {
        val nativeDependencies = llvm.nativeDependenciesToLink

        val includedBinariesLibraries = config.libraryToCache?.let { listOf(it.klib) }
                ?: nativeDependencies.filterNot { config.cachedLibraries.isLibraryCached(it) }
        val includedBinaries = includedBinariesLibraries.map { (it as? KonanLibrary)?.includedPaths.orEmpty() }.flatten()

        val libraryProvidedLinkerFlags = llvm.allNativeDependencies.map { it.linkerOpts }.flatten()

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
        val additionalLinkerArgs: List<String>
        val executable: String

        if (config.produce != CompilerOutputKind.FRAMEWORK) {
            additionalLinkerArgs = if (target.family.isAppleFamily) {
                when (config.produce) {
                    CompilerOutputKind.DYNAMIC_CACHE ->
                        listOf("-install_name", config.outputFiles.dynamicCacheInstallName)

                    else -> listOf("-dead_strip")
                }
            } else {
                emptyList()
            }
            executable = config.outputFiles.nativeBinaryFile
        } else {
            val framework = File(config.outputFile)
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

        val needsProfileLibrary = coverage.enabled
        val mimallocEnabled = config.allocationMode == AllocationMode.MIMALLOC

        val linkerInput = determineLinkerInput(objectFiles, linkerOutput)
        try {
            File(executable).delete()
            val linkerArgs = asLinkerArgs(configuration.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
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
                    outputDsymBundle = config.outputFiles.symbolicInfoFile,
                    needsProfileLibrary = needsProfileLibrary,
                    mimallocEnabled = mimallocEnabled,
                    sanitizer = config.sanitizer
            )
            (linkerInput.preLinkCommands + finalOutputCommands).forEach {
                it.logWith(logger::log)
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
            errorReporter.reportCompilationError("${e.toolName} invocation reported errors\n$extraUserInfo\n${e.message}")
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
        val caches = determineCachesToLink(llvm, llvmModuleSpecification, config)
        // Since we have several linker stages that involve caching,
        // we should detect cache usage early to report errors correctly.
        val cachingInvolved = caches.static.isNotEmpty() || caches.dynamic.isNotEmpty()
        return when {
            config.produce == CompilerOutputKind.STATIC_CACHE -> {
                // Do not link static cache dependencies.
                LinkerInput(objectFiles, CachesToLink(emptyList(), caches.dynamic), emptyList(), cachingInvolved)
            }

            shouldPerformPreLink(caches, linkerOutputKind) -> {
                val preLinkResult = config.tempFiles.create("withStaticCaches", ".o").absolutePath
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

private fun determineCachesToLink(llvm: NecessaryLlvmParts, llvmModuleSpecification: LlvmModuleSpecification, config: KonanConfig): CachesToLink {
    val staticCaches = mutableListOf<String>()
    val dynamicCaches = mutableListOf<String>()

    llvm.allCachedBitcodeDependencies.forEach { library ->
        val currentBinaryContainsLibrary = llvmModuleSpecification.containsLibrary(library)
        val cache = config.cachedLibraries.getLibraryCache(library)
                ?: error("Library $library is expected to be cached")

        // Consistency check. Generally guaranteed by implementation.
        if (currentBinaryContainsLibrary)
            error("Library ${library.libraryName} is found in both cache and current binary")

        val list = when (cache.kind) {
            CachedLibraries.Kind.DYNAMIC -> dynamicCaches
            CachedLibraries.Kind.STATIC -> staticCaches
        }

        list += cache.binariesPaths
    }
    return CachesToLink(static = staticCaches, dynamic = dynamicCaches)
}
