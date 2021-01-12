package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal fun determineLinkerOutput(context: Context): LinkerOutputKind =
        when (context.config.produce) {
            CompilerOutputKind.FRAMEWORK -> {
                val staticFramework = context.config.produceStaticFramework
                if (staticFramework) LinkerOutputKind.STATIC_LIBRARY else LinkerOutputKind.DYNAMIC_LIBRARY
            }
            CompilerOutputKind.DYNAMIC_CACHE,
            CompilerOutputKind.DYNAMIC -> LinkerOutputKind.DYNAMIC_LIBRARY
            CompilerOutputKind.STATIC_CACHE,
            CompilerOutputKind.STATIC -> LinkerOutputKind.STATIC_LIBRARY
            CompilerOutputKind.PROGRAM -> LinkerOutputKind.EXECUTABLE
            else -> TODO("${context.config.produce} should not reach native linker stage")
        }

// TODO: We have a Linker.kt file in the shared module.
internal class Linker(val context: Context) {

    private val platform = context.config.platform
    private val config = context.config.configuration
    private val linkerOutput = determineLinkerOutput(context)
    private val linker = platform.linker
    private val target = context.config.target
    private val optimize = context.shouldOptimize()
    private val debug = context.config.debug || context.config.lightDebug

    fun link(objectFiles: List<ObjectFile>) {
        val nativeDependencies = context.llvm.nativeDependenciesToLink

        val includedBinariesLibraries = if (context.config.produce.isCache) {
            context.config.librariesToCache
        } else {
            nativeDependencies.filterNot { context.config.cachedLibraries.isLibraryCached(it) }
        }
        val includedBinaries = includedBinariesLibraries.map { (it as? KonanLibrary)?.includedPaths.orEmpty() }.flatten()

        val libraryProvidedLinkerFlags = context.llvm.allNativeDependencies.map { it.linkerOpts }.flatten()

        if (context.config.produce.isCache) {
            context.config.outputFiles.tempCacheDirectory!!.mkdirs()
            saveAdditionalInfoForCache()
        }

        runLinker(objectFiles, includedBinaries, libraryProvidedLinkerFlags)

        renameOutput()
    }

    private fun saveAdditionalInfoForCache() {
        saveCacheBitcodeDependencies()
    }

    private fun saveCacheBitcodeDependencies() {
        val outputFiles = context.config.outputFiles
        val bitcodeDependenciesFile = File(outputFiles.bitcodeDependenciesFile!!)
        val bitcodeDependencies = context.config.resolvedLibraries
                .getFullList(TopologicalLibraryOrder)
                .filter {
                    require(it is KonanLibrary)
                    context.llvmImports.bitcodeIsUsed(it)
                            && it !in context.config.cacheSupport.librariesToCache // Skip loops.
                }.cast<List<KonanLibrary>>()
        bitcodeDependenciesFile.writeLines(bitcodeDependencies.map { it.uniqueName })
    }

    private fun renameOutput() {
        if (context.config.produce.isCache) {
            val outputFiles = context.config.outputFiles
            // For caches the output file is a directory. It might be created by someone else,
            // We have to delete it in order to the next renaming operation to succeed.
            java.io.File(outputFiles.mainFile).delete()
            if (!java.io.File(outputFiles.tempCacheDirectory!!.absolutePath).renameTo(java.io.File(outputFiles.mainFile)))
                outputFiles.tempCacheDirectory.deleteRecursively()
        }
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

        if (context.config.produce != CompilerOutputKind.FRAMEWORK) {
            additionalLinkerArgs = if (target.family.isAppleFamily) {
                when (context.config.produce) {
                    CompilerOutputKind.DYNAMIC_CACHE ->
                        listOf("-install_name", context.config.outputFiles.dynamicCacheInstallName)
                    else -> listOf("-dead_strip")
                }
            } else {
                emptyList()
            }
            executable = context.config.outputFiles.nativeBinaryFile
        } else {
            val framework = File(context.config.outputFile)
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

        val needsProfileLibrary = context.coverage.enabled
        val mimallocEnabled = config.get(KonanConfigKeys.ALLOCATION_MODE) == "mimalloc" &&
                target.supportsMimallocAllocator()

        val linkerInput = determineLinkerInput(objectFiles, linkerOutput)
        try {
            File(executable).delete()
            val linkerArgs = asLinkerArgs(config.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                    BitcodeEmbedding.getLinkerOptions(context.config) +
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
                    outputDsymBundle = context.config.outputFiles.symbolicInfoFile,
                    needsProfileLibrary = needsProfileLibrary,
                    mimallocEnabled = mimallocEnabled)
            (linkerInput.preLinkCommands + finalOutputCommands).forEach {
                it.logWith(context::log)
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
            context.reportCompilationError("${e.toolName} invocation reported errors\n$extraUserInfo\n${e.message}")
        }
        return executable
    }

    private fun shouldPerformPreLink(caches: CachesToLink, linkerOutputKind: LinkerOutputKind): Boolean {
        // Pre-link is only useful when producing static library. Otherwise its just a waste of time.
        val isStaticLibrary = linkerOutputKind == LinkerOutputKind.STATIC_LIBRARY &&
                context.config.produce.isFinalBinary
        val enabled = context.config.cacheSupport.preLinkCaches
        val nonEmptyCaches = caches.static.isNotEmpty()
        return isStaticLibrary && enabled && nonEmptyCaches
    }

    private fun determineLinkerInput(objectFiles: List<ObjectFile>, linkerOutputKind: LinkerOutputKind): LinkerInput {
        val caches = determineCachesToLink(context)
        // Since we have several linker stages that involve caching,
        // we should detect cache usage early to report errors correctly.
        val cachingInvolved = caches.static.isNotEmpty() || caches.dynamic.isNotEmpty()
        return when {
            context.config.produce == CompilerOutputKind.STATIC_CACHE -> {
                // Do not link static cache dependencies.
                LinkerInput(objectFiles, CachesToLink(emptyList(), caches.dynamic), emptyList(), cachingInvolved)
            }
            shouldPerformPreLink(caches, linkerOutputKind) -> {
                val preLinkResult = context.config.tempFiles.create("withStaticCaches", ".o").absolutePath
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

private fun determineCachesToLink(context: Context): CachesToLink {
    val staticCaches = mutableListOf<String>()
    val dynamicCaches = mutableListOf<String>()

    context.llvm.allCachedBitcodeDependencies.forEach { library ->
        val currentBinaryContainsLibrary = context.llvmModuleSpecification.containsLibrary(library)
        val cache = context.config.cachedLibraries.getLibraryCache(library)
                ?: error("Library $library is expected to be cached")

        // Consistency check. Generally guaranteed by implementation.
        if (currentBinaryContainsLibrary)
            error("Library ${library.libraryName} is found in both cache and current binary")

        val list = when (cache.kind) {
            CachedLibraries.Cache.Kind.DYNAMIC -> dynamicCaches
            CachedLibraries.Cache.Kind.STATIC -> staticCaches
        }

        list += cache.path
    }
    return CachesToLink(static = staticCaches, dynamic = dynamicCaches)
}
