/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.native.interop.gen.jvm.InternalInteropOptions
import org.jetbrains.kotlin.native.interop.gen.jvm.Interop
import org.jetbrains.kotlin.native.interop.tool.*

// TODO: this function should eventually be eliminated from 'utilities'. 
// The interaction of interop and the compiler should be streamlined.

/**
 * @return null if there is no need in compiler invocation.
 * Otherwise returns array of compiler args.
 */
fun invokeInterop(flavor: String, args: Array<String>, runFromDaemon: Boolean): Array<String>? {
    check(flavor == "native") {
        "wasm target in Kotlin/Native is removed. See https://kotl.in/native-targets-tiers"
    }
    val arguments = CInteropArguments()
    arguments.argParser.parse(args)
    val outputFileName = arguments.output
    val noDefaultLibs = arguments.nodefaultlibs || arguments.nodefaultlibsDeprecated
    val noEndorsedLibs = arguments.noendorsedlibs
    val purgeUserLibs = arguments.purgeUserLibs
    val nopack = arguments.nopack
    val temporaryFilesDir = arguments.tempDir
    val moduleName = arguments.moduleName
    val shortModuleName = arguments.shortModuleName

    val buildDir = File("$outputFileName-build")
    val generatedDir = File(buildDir, "kotlin")
    val nativesDir = File(buildDir,"natives")
    val manifest = File(buildDir, "manifest.properties")
    val cstubsName ="cstubs"
    val libraries = arguments.library
    val repos = arguments.repo
    val targetRequest = arguments.target
    val target = PlatformManager(
        KotlinNativePaths.homePath.absolutePath,
        konanDataDir = arguments.konanDataDir).targetManager(targetRequest).target

    val cinteropArgsToCompiler = Interop().interop("native", args,
            InternalInteropOptions(generatedDir.absolutePath,
                    nativesDir.absolutePath,manifest.path,
                    cstubsName
            ),
            runFromDaemon
    ) ?: return null // There is no need in compiler invocation if we're generating only metadata.

    val nativeStubs =
            arrayOf("-native-library", File(nativesDir, "$cstubsName.bc").path)

    return arrayOf(
        generatedDir.path,
        "-produce", "library",
        "-o", outputFileName,
        "-target", target.visibleName,
        "-manifest", manifest.path,
        "-opt-in=kotlin.native.SymbolNameIsInternal",
        "-Xtemporary-files-dir=$temporaryFilesDir") +
        nativeStubs +
        cinteropArgsToCompiler +
        libraries.flatMap { listOf("-library", it) } +
        repos.flatMap { listOf("-repo", it) } +
        (if (noDefaultLibs) arrayOf("-$NODEFAULTLIBS") else emptyArray()) +
        (if (noEndorsedLibs) arrayOf("-$NOENDORSEDLIBS") else emptyArray()) +
        (if (purgeUserLibs) arrayOf("-$PURGE_USER_LIBS") else emptyArray()) +
        (if (nopack) arrayOf("-$NOPACK") else emptyArray()) +
        moduleName?.let { arrayOf("-module-name", it) }.orEmpty() +
        shortModuleName?.let { arrayOf("${K2NativeCompilerArguments.SHORT_MODULE_NAME_ARG}=$it") }.orEmpty() +
        "-library-version=${arguments.libraryVersion}" +
        arguments.kotlincOption
}


