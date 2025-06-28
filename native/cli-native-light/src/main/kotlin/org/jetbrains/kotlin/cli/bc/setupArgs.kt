package org.jetbrains.kotlin.cli.bc

import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.TestRunnerKind
import org.jetbrains.kotlin.backend.konan.report
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.getModuleNameForSource
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun CompilerConfiguration.setupFromArguments(arguments: K2NativeCompilerArguments) = with(KonanConfigKeys) {
    val commonSources = arguments.commonSources?.toSet().orEmpty().map { it.absoluteNormalizedFile() }
    val hmppModuleStructure = get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
    arguments.freeArgs.forEach {
        addKotlinSourceRoot(it, isCommon = it.absoluteNormalizedFile() in commonSources, hmppModuleStructure?.getModuleNameForSource(it))
    }

    arguments.kotlinHome?.let { put(KONAN_HOME, it) }

    put(NODEFAULTLIBS, arguments.nodefaultlibs || !arguments.libraryToAddToCache.isNullOrEmpty())
    put(NOENDORSEDLIBS, arguments.noendorsedlibs || !arguments.libraryToAddToCache.isNullOrEmpty())
    put(NOSTDLIB, arguments.nostdlib || !arguments.libraryToAddToCache.isNullOrEmpty())
    put(NOPACK, arguments.nopack)
    put(NOMAIN, arguments.nomain)
    put(LIBRARY_FILES, arguments.libraries.toNonNullList())
    put(LINKER_ARGS, arguments.linkerArguments.toNonNullList() +
            arguments.singleLinkerArguments.toNonNullList())
    arguments.moduleName?.let { put(MODULE_NAME, it) }

    arguments.target?.let { put(TARGET, it) }

    put(INCLUDED_BINARY_FILES, arguments.includeBinaries.toNonNullList())
    put(NATIVE_LIBRARY_FILES, arguments.nativeLibraries.toNonNullList())

    // TODO: Collect all the explicit file names into an object
    // and teach the compiler to work with temporaries and -save-temps.

    arguments.outputName?.let { put(OUTPUT, it) }
    val outputKind = CompilerOutputKind.valueOf(
        (arguments.produce ?: "program").uppercase())
    put(PRODUCE, outputKind)
    putIfNotNull(HEADER_KLIB, arguments.headerKlibPath)

    arguments.mainPackage?.let { put(ENTRY, it) }
    arguments.manifestFile?.let { put(MANIFEST_FILE, it) }
    arguments.runtimeFile?.let { put(RUNTIME_FILE, it) }
    arguments.temporaryFilesDir?.let { put(TEMPORARY_FILES_DIR, it) }

    put(LIST_TARGETS, arguments.listTargets)

    put(EXPORT_KDOC, arguments.exportKDoc)

    put(PRINT_IR, arguments.printIr)
    put(PRINT_BITCODE, arguments.printBitCode)
    put(PRINT_FILES, arguments.printFiles)

    put(PURGE_USER_LIBS, arguments.purgeUserLibs)

    putIfNotNull(WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO, arguments.writeDependenciesOfProducedKlibTo)

    if (arguments.verifyCompiler != null)
        put(VERIFY_COMPILER, arguments.verifyCompiler == "true")
    put(VERIFY_BITCODE, arguments.verifyBitCode)

    put(ENABLE_ASSERTIONS, arguments.enableAssertions)

    when {
        arguments.generateWorkerTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.WORKER)
        arguments.generateTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.MAIN_THREAD)
        arguments.generateNoExitTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.MAIN_THREAD_NO_EXIT)
        else -> put(GENERATE_TEST_RUNNER, TestRunnerKind.NONE)
    }

    if (arguments.friendModules != null)
        put(FRIEND_MODULES, arguments.friendModules!!.split(File.pathSeparator).filterNot(String::isEmpty))

    if (arguments.refinesPaths != null)
        put(REFINES_MODULES, arguments.refinesPaths!!.filterNot(String::isEmpty))

    put(EXPORTED_LIBRARIES, selectExportedLibraries(this@setupFromArguments, arguments, outputKind))
    put(INCLUDED_LIBRARIES, selectIncludes(this@setupFromArguments, arguments, outputKind))

    parseShortModuleName(arguments, this@setupFromArguments, outputKind)?.let {
        put(SHORT_MODULE_NAME, it)
    }
    put(FAKE_OVERRIDE_VALIDATOR, arguments.fakeOverrideValidator)


    arguments.externalDependencies?.let { put(EXTERNAL_DEPENDENCIES, it) }

    setupPartialLinkageConfig(
        mode = arguments.partialLinkageMode,
        logLevel = arguments.partialLinkageLogLevel,
        compilerModeAllowsUsingPartialLinkage = outputKind != CompilerOutputKind.LIBRARY, // Don't run PL when producing KLIB.
        onWarning = { report(WARNING, it) },
        onError = { report(ERROR, it) }
    )

    putIfNotNull(SERIALIZED_DEPENDENCIES, parseSerializedDependencies(arguments, this@setupFromArguments))
    putIfNotNull(SAVE_DEPENDENCIES_PATH, arguments.saveDependenciesPath)
    putIfNotNull(KONAN_DATA_DIR, arguments.konanDataDir)

    if (arguments.manifestNativeTargets != null)
        putIfNotNull(MANIFEST_NATIVE_TARGETS, parseManifestNativeTargets(arguments.manifestNativeTargets!!))

}

private fun String.absoluteNormalizedFile() = java.io.File(this).absoluteFile.normalize()

private fun Array<String>?.toNonNullList() = this?.asList().orEmpty()

private fun selectExportedLibraries(
    configuration: CompilerConfiguration,
    arguments: K2NativeCompilerArguments,
    outputKind: CompilerOutputKind
): List<String> {
    val exportedLibraries = arguments.exportedLibraries?.toList().orEmpty()

    return if (exportedLibraries.isNotEmpty() && outputKind != CompilerOutputKind.FRAMEWORK &&
        outputKind != CompilerOutputKind.STATIC && outputKind != CompilerOutputKind.DYNAMIC) {
        configuration.report(STRONG_WARNING,
                             "-Xexport-library is only supported when producing frameworks or native libraries, " +
                                     "but the compiler is producing ${outputKind.name.lowercase()}")

        emptyList()
    } else {
        exportedLibraries
    }
}

private fun selectIncludes(
    configuration: CompilerConfiguration,
    arguments: K2NativeCompilerArguments,
    outputKind: CompilerOutputKind
): List<String> {
    val includes = arguments.includes?.toList().orEmpty()

    return if (includes.isNotEmpty() && outputKind == CompilerOutputKind.LIBRARY) {
        configuration.report(
            ERROR,
            "The ${K2NativeCompilerArguments::includes.cliArgument} flag is not supported when producing ${outputKind.name.lowercase()}"
        )
        emptyList()
    } else {
        includes
    }
}

private fun parseCachedLibraries(
    arguments: K2NativeCompilerArguments,
    configuration: CompilerConfiguration
): Map<String, String> = arguments.cachedLibraries?.asList().orEmpty().mapNotNull {
    val libraryAndCache = it.split(",")
    if (libraryAndCache.size != 2) {
        configuration.report(
            ERROR,
            "incorrect ${K2NativeCompilerArguments::cachedLibraries.cliArgument} format: expected '<library>,<cache>', got '$it'"
        )
        null
    } else {
        libraryAndCache[0] to libraryAndCache[1]
    }
}.toMap()

// TODO: Support short names for current module in ObjC export and lift this limitation.
private fun parseShortModuleName(
    arguments: K2NativeCompilerArguments,
    configuration: CompilerConfiguration,
    outputKind: CompilerOutputKind
): String? {
    val input = arguments.shortModuleName

    return if (input != null && outputKind != CompilerOutputKind.LIBRARY) {
        configuration.report(
            STRONG_WARNING,
            "${K2NativeCompilerArguments::shortModuleName.cliArgument} is only supported when producing a Kotlin library, " +
                    "but the compiler is producing ${outputKind.name.lowercase()}"
        )
        null
    } else {
        input
    }
}

private fun parseSerializedDependencies(
    arguments: K2NativeCompilerArguments,
    configuration: CompilerConfiguration
): String? {
    if (!arguments.serializedDependencies.isNullOrEmpty() && arguments.compileFromBitcode.isNullOrEmpty()) {
        configuration.report(STRONG_WARNING,
                             "Providing serialized dependencies only works in conjunction with a bitcode file to compile.")
    }
    return arguments.serializedDependencies
}

private fun CompilerConfiguration.parseManifestNativeTargets(targetStrings: Array<String>): Collection<KonanTarget> {
    val trimmedTargetStrings = targetStrings.map { it.trim() }
    val (recognizedTargetNames, unrecognizedTargetNames) = trimmedTargetStrings.partition { it in KonanTarget.predefinedTargets.keys }

    if (unrecognizedTargetNames.isNotEmpty()) {
        report(
            WARNING,
            """
                    The following target names passed to the -Xmanifest-native-targets are not recognized:
                    ${unrecognizedTargetNames.joinToString(separator = ", ")}
                    
                    List of known target names:
                    ${KonanTarget.predefinedTargets.keys.joinToString(separator = ", ")}
                """.trimIndent()
        )
    }

    return recognizedTargetNames.map { KonanTarget.predefinedTargets[it]!! }
}