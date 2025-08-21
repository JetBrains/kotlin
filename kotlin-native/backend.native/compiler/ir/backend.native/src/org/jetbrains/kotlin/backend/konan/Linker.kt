package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.util.toObsoleteKind
import org.jetbrains.kotlin.config.nativeBinaryOptions.AndroidProgramType
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.uniqueName

internal fun determineLinkerOutput(context: PhaseContext): LinkerOutputKind =
        when (context.config.produce) {
            CompilerOutputKind.FRAMEWORK -> {
                val staticFramework = context.config.produceStaticFramework
                if (staticFramework) LinkerOutputKind.STATIC_LIBRARY else LinkerOutputKind.DYNAMIC_LIBRARY
            }
            CompilerOutputKind.TEST_BUNDLE,
            CompilerOutputKind.DYNAMIC_CACHE,
            CompilerOutputKind.DYNAMIC -> LinkerOutputKind.DYNAMIC_LIBRARY
            CompilerOutputKind.STATIC_CACHE,
            CompilerOutputKind.STATIC -> LinkerOutputKind.STATIC_LIBRARY
            CompilerOutputKind.PROGRAM -> run {
                if (context.config.target.family == Family.ANDROID) {
                    val configuration = context.config.configuration
                    val androidProgramType = configuration.get(BinaryOptions.androidProgramType) ?: AndroidProgramType.Default
                    when (androidProgramType) {
                        AndroidProgramType.Standalone -> LinkerOutputKind.EXECUTABLE
                        AndroidProgramType.NativeActivity -> LinkerOutputKind.DYNAMIC_LIBRARY
                    }
                }
                LinkerOutputKind.EXECUTABLE
            }
            else -> TODO("${context.config.produce} should not reach native linker stage")
        }

// TODO: We have a Linker.kt file in the shared module.
internal class Linker(
        private val config: KonanConfig,
        private val linkerOutput: LinkerOutputKind,
        private val outputFiles: OutputFiles,
        private val tempFiles: TempFiles,
) {
    private val platform = config.platform
    private val linker = platform.linker
    private val target = config.target
    private val optimize = config.optimizationsEnabled
    private val debug = config.debug || config.lightDebug

    fun linkCommands(
            outputFile: String,
            objectFiles: List<ObjectFile>,
            dependenciesTrackingResult: DependenciesTrackingResult,
            caches: ResolvedCacheBinaries,
    ): List<Command> {
        val nativeDependencies = dependenciesTrackingResult.nativeDependenciesToLink

        val includedBinariesLibraries = config.libraryToCache?.let { listOf(it.klib) }
                ?: nativeDependencies.filterNot { config.cachedLibraries.isLibraryCached(it) }
        val includedBinaries = includedBinariesLibraries.map { (it as? KonanLibrary)?.includedPaths.orEmpty() }.flatten()

        val libraryProvidedLinkerFlags = dependenciesTrackingResult.allNativeDependencies.map { it.linkerOpts }.flatten()
        return runLinker(outputFile, objectFiles, includedBinaries, libraryProvidedLinkerFlags, caches)
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
            caches: ResolvedCacheBinaries,
    ): List<Command> {
        val additionalLinkerArgs: List<String>
        val executable: String

        when (config.produce) {
            CompilerOutputKind.TEST_BUNDLE -> {
                val bundleDir = File(outputFile)
                val name = bundleDir.name.removeSuffix(config.produce.suffix())
                require(target.family.isAppleFamily)
                val bundleRelativePath = if (target.family == Family.OSX) "Contents/MacOS/$name" else name
                additionalLinkerArgs = listOf("-bundle", "-dead_strip")
                val bundlePath = bundleDir.child(bundleRelativePath)
                bundlePath.parentFile.mkdirs()
                executable = bundlePath.absolutePath
            }
            CompilerOutputKind.FRAMEWORK -> {
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
            else -> {
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
            }
        }
        File(executable).delete()

        val linkerArgs = asLinkerArgs(config.configuration.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                caches.dynamic +
                libraryProvidedLinkerFlags + additionalLinkerArgs

        return with(linker) {
            LinkerArguments(
                    tempFiles = tempFiles,
                    objectFiles = objectFiles,
                    executable = executable,
                    libraries = linker.linkStaticLibraries(includedBinaries) + caches.static,
                    linkerArgs = linkerArgs,
                    optimize = optimize,
                    debug = debug,
                    kind = linkerOutput,
                    outputDsymBundle = outputFiles.symbolicInfoFile,
                    sanitizer = config.sanitizer?.toObsoleteKind(),
            ).finalLinkCommands()
        }
    }
}

internal fun runLinkerCommands(context: PhaseContext, commands: List<Command>, cachingInvolved: Boolean) = try {
    commands.forEach {
        it.logWith(context::log)
        it.execute()
    }
} catch (e: KonanExternalToolFailure) {
    val extraUserInfo = if (cachingInvolved)
        """
                    Please try to disable compiler caches and rerun the build. To disable compiler caches, add the following line to the gradle.properties file in the project's root directory:
                        
                        kotlin.native.cacheKind.${context.config.target.presetName}=none
                        
                    Also, consider filing an issue with full Gradle log here: https://kotl.in/issue
                    """.trimIndent()
    else null

    val extraUserSetupInfo = run {
        context.config.resolvedLibraries.getFullResolvedList()
                .filter { it.library.isCInteropLibrary() }
                .mapNotNull { library ->
                    library.library.manifestProperties["userSetupHint"]?.let {
                        "From ${library.library.uniqueName}:\n$it".takeIf { it.isNotEmpty() }
                    }
                }
                .mapIndexed { index, message -> "$index. $message" }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(separator = "\n\n")
                ?.let {
                    "It seems your project produced link errors.\nProposed solutions:\n\n$it\n"
                }
    }

    val extraInfo = listOfNotNull(extraUserInfo, extraUserSetupInfo).joinToString(separator = "\n")

    context.reportCompilationError("${e.toolName} invocation reported errors\n$extraInfo\n${e.message}")
}
