package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.*
import java.io.File

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

internal data class LinkerOutputs(
        val binaryFile: File,
        val dSymFile: File?,
        val installName: String?
)

// TODO: We have a Linker.kt file in the shared module.
internal class Linker(
        private val context: PhaseContext,
) {

    private val config = context.config
    private val platform = config.platform
    private val linkerOutput = determineLinkerOutput(context)
    private val linker = platform.linker
    private val target = config.target
    private val optimize = context.shouldOptimize()
    private val debug = config.debug || config.lightDebug

    fun link(
            linkerOutputs: LinkerOutputs,
            objectFiles: List<File>,
            dependenciesTrackingResult: DependenciesTrackingResult,
            isCoverageEnabled: Boolean
    ) {
        val nativeDependencies = dependenciesTrackingResult.nativeDependenciesToLink

        val includedBinariesLibraries = config.libraryToCache?.let { listOf(it.klib) }
                ?: nativeDependencies.filterNot { config.cachedLibraries.isLibraryCached(it) }
        val includedBinaries = includedBinariesLibraries.map { (it as? KonanLibrary)?.includedPaths.orEmpty() }.flatten()

        val libraryProvidedLinkerFlags = dependenciesTrackingResult.allNativeDependencies.map { it.linkerOpts }.flatten()
        val caches = determineCachesToLink(context, dependenciesTrackingResult)
        runLinker(objectFiles, includedBinaries, libraryProvidedLinkerFlags, caches, linkerOutputs, isCoverageEnabled)
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

    private fun runLinker(
            objectFiles: List<File>,
            includedBinaries: List<String>,
            libraryProvidedLinkerFlags: List<String>,
            caches: CachesToLink,
            linkerOutputs: LinkerOutputs,
            isCoverageEnabled: Boolean
    ): ExecutableFile {
        val additionalLinkerArgs: List<String> = linkerOutputs.installName?.let { listOf("-install_name, $it") } ?: emptyList()
        val executable: String = linkerOutputs.binaryFile.absolutePath

        val mimallocEnabled = config.allocationMode == AllocationMode.MIMALLOC

        val linkerInput = determineLinkerInput(objectFiles, caches)
        try {
            File(executable).delete()
            val linkerArgs = asLinkerArgs(config.configuration.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                    BitcodeEmbedding.getLinkerOptions(config) +
                    linkerInput.caches.dynamic +
                    libraryProvidedLinkerFlags + additionalLinkerArgs

            val finalOutputCommands = linker.finalLinkCommands(
                    objectFiles = linkerInput.objectFiles.map { it.absolutePath },
                    executable = executable,
                    libraries = linker.linkStaticLibraries(includedBinaries) + linkerInput.caches.static,
                    linkerArgs = linkerArgs,
                    optimize = optimize,
                    debug = debug,
                    kind = linkerOutput,
                    outputDsymBundle = linkerOutputs.dSymFile?.absolutePath,
                    needsProfileLibrary = isCoverageEnabled,
                    mimallocEnabled = mimallocEnabled,
                    sanitizer = config.sanitizer
            )
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
                config.isFinalBinary
        val enabled = config.cacheSupport.preLinkCaches
        val nonEmptyCaches = caches.static.isNotEmpty()
        return isStaticLibrary && enabled && nonEmptyCaches
    }

    private fun determineLinkerInput(
            objectFiles: List<File>,
            caches: CachesToLink,
    ): LinkerInput {
        // Since we have several linker stages that involve caching,
        // we should detect cache usage early to report errors correctly.
        val cachingInvolved = caches.static.isNotEmpty() || caches.dynamic.isNotEmpty()
        return when {
            config.produce == CompilerOutputKind.STATIC_CACHE -> {
                // Do not link static cache dependencies.
                LinkerInput(objectFiles, CachesToLink(emptyList(), caches.dynamic), emptyList(), cachingInvolved)
            }
            else -> LinkerInput(objectFiles, caches, emptyList(), cachingInvolved)
        }
    }

    fun preLinkStaticCaches(
            inputObjectFiles: List<File>,
            outputObjectFile: File,
            dependenciesTrackingResult: DependenciesTrackingResult,
    ) {
        val caches = determineCachesToLink(context, dependenciesTrackingResult)
        val preLinkCommands = linker.preLinkCommands(inputObjectFiles.map { it.absolutePath } + caches.static, outputObjectFile.absolutePath)
        preLinkCommands.forEach {
            it.logWith(context::log)
            it.execute()
        }
    }
}

private class LinkerInput(
        val objectFiles: List<File>,
        val caches: CachesToLink,
        val preLinkCommands: List<Command>,
        val cachingInvolved: Boolean
)

internal class CachesToLink(val static: List<String>, val dynamic: List<String>)

internal fun determineCachesToLink(
        context: PhaseContext,
        dependenciesTrackingResult: DependenciesTrackingResult,
): CachesToLink {
    val staticCaches = mutableListOf<String>()
    val dynamicCaches = mutableListOf<String>()

    dependenciesTrackingResult.allCachedBitcodeDependencies.forEach { dependency ->
        val library = dependency.library
        val cache = context.config.cachedLibraries.getLibraryCache(library)
                ?: error("Library $library is expected to be cached")

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
