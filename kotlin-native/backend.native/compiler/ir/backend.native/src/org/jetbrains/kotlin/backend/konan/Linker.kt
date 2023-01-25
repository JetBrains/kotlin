package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.TempFiles
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

// TODO: We have a Linker.kt file in the shared module.
internal class Linker(
        private val context: PhaseContext,
        private val isCoverageEnabled: Boolean = false,
        private val tempFiles: TempFiles,
        private val outputFiles: OutputFiles,
) {

    private val config = context.config
    private val platform = config.platform
    private val linkerOutput = determineLinkerOutput(context)
    private val linker = platform.linker
    private val target = config.target
    private val optimize = context.shouldOptimize()
    private val debug = config.debug || config.lightDebug

    fun link(
            outputFile: String,
            objectFiles: List<ObjectFile>,
            dependenciesTrackingResult: DependenciesTrackingResult
    ) {
        val nativeDependencies = dependenciesTrackingResult.nativeDependenciesToLink

        val includedBinariesLibraries = config.libraryToCache?.let { listOf(it.klib) }
                ?: nativeDependencies.filterNot { config.cachedLibraries.isLibraryCached(it) }
        val includedBinaries = includedBinariesLibraries.map { (it as? KonanLibrary)?.includedPaths.orEmpty() }.flatten()

        val libraryProvidedLinkerFlags = dependenciesTrackingResult.allNativeDependencies.map { it.linkerOpts }.flatten()

        runLinker(outputFile, objectFiles, includedBinaries, libraryProvidedLinkerFlags, dependenciesTrackingResult)
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
            outputFile: String,
            objectFiles: List<ObjectFile>,
            includedBinaries: List<String>,
            libraryProvidedLinkerFlags: List<String>,
            dependenciesTrackingResult: DependenciesTrackingResult,
    ): ExecutableFile {
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
            val framework = File(outputFile)
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

        val mimallocEnabled = config.allocationMode == AllocationMode.MIMALLOC

        val linkerInput = determineLinkerInput(objectFiles, linkerOutput, dependenciesTrackingResult)
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

    private fun determineLinkerInput(
            objectFiles: List<ObjectFile>,
            linkerOutputKind: LinkerOutputKind,
            dependenciesTrackingResult: DependenciesTrackingResult,
    ): LinkerInput {
        val caches = resolveCacheBinaries(context.config.cachedLibraries, dependenciesTrackingResult)
        // Since we have several linker stages that involve caching,
        // we should detect cache usage early to report errors correctly.
        val cachingInvolved = caches.static.isNotEmpty() || caches.dynamic.isNotEmpty()
        return when {
            config.produce == CompilerOutputKind.STATIC_CACHE -> {
                // Do not link static cache dependencies.
                LinkerInput(objectFiles, ResolvedCacheBinaries(emptyList(), caches.dynamic), emptyList(), cachingInvolved)
            }
            shouldPerformPreLink(config, caches, linkerOutputKind) -> {
                val preLinkResult = tempFiles.create("withStaticCaches", ".o").absolutePath
                val preLinkCommands = linker.preLinkCommands(objectFiles + caches.static, preLinkResult)
                LinkerInput(listOf(preLinkResult), ResolvedCacheBinaries(emptyList(), caches.dynamic), preLinkCommands, cachingInvolved)
            }
            else -> LinkerInput(objectFiles, caches, emptyList(), cachingInvolved)
        }
    }
}

private class LinkerInput(
        val objectFiles: List<ObjectFile>,
        val caches: ResolvedCacheBinaries,
        val preLinkCommands: List<Command>,
        val cachingInvolved: Boolean
)