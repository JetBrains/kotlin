package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
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

// TODO: We have a Linker.kt file in the shared module.
internal class Linker(
        private val config: KonanConfig,
) {
    private val platform = config.platform
    private val linker = platform.linker
    private val optimize = config.optimizationsEnabled
    private val debug = config.debug || config.lightDebug

    fun linkCommands(
            outputFile: java.io.File,
            objectFiles: List<ObjectFile>,
            linkerOutput: LinkerOutputKind,
            dependenciesTrackingResult: DependenciesTrackingResult,
            caches: ResolvedCacheBinaries,
            symbolicInfoFile: String,
            installName: String? = null,
            isCoverageEnabled: Boolean = false,
    ): List<Command> {
        val nativeDependencies = dependenciesTrackingResult.nativeDependenciesToLink

        val includedBinariesLibraries = config.libraryToCache?.let { listOf(it.klib) }
                ?: nativeDependencies.filterNot { config.cachedLibraries.isLibraryCached(it) }
        val includedBinaries = includedBinariesLibraries.map { (it as? KonanLibrary)?.includedPaths.orEmpty() }.flatten()

        val libraryProvidedLinkerFlags = dependenciesTrackingResult.allNativeDependencies.map { it.linkerOpts }.flatten()

        val installNameArgument = installName?.let { listOf("-install_name", it) } ?: emptyList()

        // TODO: flag to control dead strip
        val linkerArgs = asLinkerArgs(config.configuration.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                BitcodeEmbedding.getLinkerOptions(config) +
                caches.dynamic +
                libraryProvidedLinkerFlags + installNameArgument

        return linker.finalLinkCommands(
                objectFiles = objectFiles,
                executable = outputFile.canonicalPath,
                libraries = linker.linkStaticLibraries(includedBinaries) + caches.static,
                linkerArgs = linkerArgs,
                optimize = optimize,
                debug = debug,
                kind = linkerOutput,
                outputDsymBundle = symbolicInfoFile,
                needsProfileLibrary = isCoverageEnabled,
                mimallocEnabled = config.allocationMode == AllocationMode.MIMALLOC,
                sanitizer = config.sanitizer
        )
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
}

internal fun runLinkerCommands(context: PhaseContext, commands: List<Command>, cachingInvolved: Boolean) = try {
    commands.forEach {
        it.logWith(context::log)
        it.execute()
    }
} catch (e: KonanExternalToolFailure) {
    val extraUserInfo =
            if (cachingInvolved)
                """
                        Please try to disable compiler caches and rerun the build. To disable compiler caches, add the following line to the gradle.properties file in the project's root directory:
                            
                            kotlin.native.cacheKind.${context.config.target.presetName}=none
                            
                        Also, consider filing an issue with full Gradle log here: https://kotl.in/issue
                        """.trimIndent()
            else ""
    context.reportCompilationError("${e.toolName} invocation reported errors\n$extraUserInfo\n${e.message}")
}