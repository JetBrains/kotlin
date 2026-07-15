package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.util.toObsoleteKind
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.nativeBinaryOptions.AndroidProgramType
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.library.components.nativeIncludedBinaries
import org.jetbrains.kotlin.konan.library.linkerOpts
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.LinkerArguments
import org.jetbrains.kotlin.konan.target.LinkerOutputKind
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.uniqueName
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name
import kotlin.io.path.pathString

private data class ExecutableTarget(
        val path: String,
        val flags: List<String>
)

private fun List<String>.asLinkerArgs(useCompilerDriverAsLinker: Boolean): List<String> {
    if (useCompilerDriverAsLinker) return this
    return flatMap { arg ->
        if (arg.startsWith("-Wl,")) {
            arg.substring(4).split(',')
        } else {
            listOf(arg)
        }
    }
}

internal fun determineLinkerOutput(context: NativeBackendPhaseContext): LinkerOutputKind =
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
        private val config: NativeSecondStageCompilationConfig,
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
            extraLinkerFlags: List<String> = emptyList()
    ): List<Command> {
        val nativeDependencies = dependenciesTrackingResult.nativeDependenciesToLink

        val includedBinariesLibraries = config.libraryToCache?.let { listOf(it.klib) }
                ?: nativeDependencies.filterNot { config.cachedLibraries.isLibraryCached(it) }
        val includedBinaries = includedBinariesLibraries.flatMap { library ->
            library.nativeIncludedBinaries(config.target)?.nativeIncludedBinaryFilePaths?.map { it.pathString }.orEmpty()
        }

        val libraryProvidedLinkerFlags = dependenciesTrackingResult.allNativeDependencies.flatMap { it.linkerOpts }
        return runLinker(outputFile, objectFiles, includedBinaries, libraryProvidedLinkerFlags, extraLinkerFlags, caches)
    }

    private fun resolveExecutableTarget(outputFile: String): ExecutableTarget = when (config.produce) {
        CompilerOutputKind.TEST_BUNDLE -> {
            require(target.family.isAppleFamily) {
                "Test Bundle requires a target belonging to Apple family"
            }
            val bundleDir = Path(outputFile)
            val name = bundleDir.name.removeSuffix(config.produce.suffix())
            val bundleRelativePath = if (target.family == Family.OSX) "Contents/MacOS/$name" else name

            ExecutableTarget(
                    path = bundleDir.resolve(bundleRelativePath).absolutePathString(),
                    flags = listOf("-bundle", "-dead_strip")
            )
        }
        CompilerOutputKind.FRAMEWORK -> {
            val framework = Path(outputFile)
            val dylibName = framework.name.removeSuffix(".framework")
            val dylibRelativePath = when (target.family) {
                Family.IOS, Family.TVOS, Family.WATCHOS -> dylibName
                Family.OSX -> "Versions/A/$dylibName"
                else -> error("Unsupported target family for Framework: $target")
            }

            ExecutableTarget(
                    path = framework.resolve(dylibRelativePath).absolutePathString(),
                    flags = listOf("-dead_strip", "-install_name", "@rpath/${framework.name}/$dylibRelativePath")
            )
        }
        else -> {
            val flags = if (target.family.isAppleFamily) {
                when (config.produce) {
                    CompilerOutputKind.DYNAMIC_CACHE -> listOf("-install_name", outputFiles.dynamicCacheInstallName)
                    else -> listOf("-dead_strip")
                }
            } else emptyList()

            ExecutableTarget(outputFiles.nativeBinaryFile, flags)
        }
    }

    private fun prepareFileSystem(executablePath: String) {
        val file = Path(executablePath)
        file.parent.createDirectories()
        file.deleteIfExists()
    }

    private fun runLinker(
            outputFile: String,
            objectFiles: List<ObjectFile>,
            includedBinaries: List<String>,
            libraryProvidedLinkerFlags: List<String>,
            extraLinkerFlags: List<String>,
            caches: ResolvedCacheBinaries,
    ): List<Command> {

        val [executablePath, produceKindFlags] = resolveExecutableTarget(outputFile).also {
            prepareFileSystem(it.path)
        }

        val driverLinkerFlags = config.configuration.getNotNull(NativeConfigurationKeys.LINKER_ARGS)
                .asLinkerArgs(linker.useCompilerDriverAsLinker)
        val linkerArgs = driverLinkerFlags + libraryProvidedLinkerFlags + produceKindFlags + extraLinkerFlags

        return with(linker) {
            LinkerArguments(
                    tempFiles = tempFiles,
                    objectFiles = objectFiles,
                    executable = executablePath,
                    staticLibraries = linker.linkStaticLibraries(includedBinaries) + caches.static,
                    dynamicLibraries = caches.dynamic,
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

internal fun runLinkerCommands(context: NativeBackendPhaseContext, commands: List<Command>, cachingInvolved: Boolean) = try {
    commands.forEach {
        it.logWith(context::log)
        it.execute()
    }
} catch (e: KonanExternalToolFailure) {
    val extraUserInfo = if (cachingInvolved) {
        val incrementalCompilationEnabled = context.config.configuration[CommonConfigurationKeys.INCREMENTAL_COMPILATION] == true
        val workaround = when {
            incrementalCompilationEnabled ->
                "incremental compilation (kotlin.incremental.native=false)"
            else ->
                "compiler caches (https://kotl.in/disable-native-cache)"
        }
        """
            Please try to disable $workaround and rerun the build.

            Also, consider filing an issue with full Gradle log here: https://kotl.in/issue
            """.trimIndent()
    } else null

    val extraUserSetupInfo = run {
        context.config.resolvedLibraries.getFullList()
                .filter { it.isCInteropLibrary() }
                .mapNotNull { library ->
                    library.manifestProperties["userSetupHint"]?.let {
                        "From ${library.uniqueName}:\n$it".takeIf { it.isNotEmpty() }
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
