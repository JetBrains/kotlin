/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.native.interop.gen.jvm.interop
import org.jetbrains.kotlin.native.interop.tool.*

// TODO: this function should eventually be eliminated from 'utilities'. 
// The interaction of interop and the compiler should be streamlined.

/**
 * @return null if there is no need in compiler invocation.
 * Otherwise returns array of compiler args.
 */
fun invokeInterop(flavor: String, args: Array<String>): Array<String>? {
    val arguments = if (flavor == "native") CInteropArguments() else JSInteropArguments()
    arguments.argParser.parse(args)
    val outputFileName = arguments.output
    val noDefaultLibs = arguments.nodefaultlibs || arguments.nodefaultlibsDeprecated
    val noEndorsedLibs = arguments.noendorsedlibs
    val purgeUserLibs = arguments.purgeUserLibs
    val temporaryFilesDir = arguments.tempDir

    val buildDir = File("$outputFileName-build")
    val generatedDir = File(buildDir, "kotlin")
    val nativesDir = File(buildDir, "natives")
    val manifest = File(buildDir, "manifest.properties")
    val additionalArgs = listOf(
            "-generated", generatedDir.path,
            "-natives", nativesDir.path,
            "-flavor", flavor
    )
    val cstubsName ="cstubs"
    val libraries = arguments.library
    val repos = arguments.repo
    val targetRequest = if (arguments is CInteropArguments) arguments.target
        else (arguments as JSInteropArguments).target
    val target = PlatformManager().targetManager(targetRequest).target

    val cinteropArgsToCompiler = interop(flavor, args + additionalArgs,
            listOfNotNull(
                    "manifest" to manifest.path,
                    ("cstubsName" to cstubsName).takeIf { flavor == "native" }
            ).toMap()
    ) ?: return null // There is no need in compiler invocation if we're generating only metadata.

    val nativeStubs =
        if (flavor == "wasm")
            arrayOf("-include-binary", File(nativesDir, "js_stubs.js").path)
        else
            arrayOf("-native-library", File(nativesDir, "$cstubsName.bc").path)

    return arrayOf(
        generatedDir.path,
        "-produce", "library",
        "-o", outputFileName,
        "-target", target.visibleName,
        "-manifest", manifest.path,
        "-Xtemporary-files-dir=$temporaryFilesDir") +
        nativeStubs +
        cinteropArgsToCompiler +
        libraries.flatMap { listOf("-library", it) } +
        repos.flatMap { listOf("-repo", it) } +
        (if (noDefaultLibs) arrayOf("-$NODEFAULTLIBS") else emptyArray()) +
        (if (noEndorsedLibs) arrayOf("-$NOENDORSEDLIBS") else emptyArray()) +
        (if (purgeUserLibs) arrayOf("-$PURGE_USER_LIBS") else emptyArray())
}


