/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.getCompileOnlyBenchmarksOpts
import org.jetbrains.kotlin.getNativeProgramExtension
import org.jetbrains.kotlin.mingwPath

plugins {
    id("compile-benchmarking")
}

val dist = file(findProperty("kotlin.native.home") ?: "dist")
val toolSuffix = if (System.getProperty("os.name").startsWith("Windows")) ".bat" else ""
val binarySuffix = getNativeProgramExtension()
val videoplayerDir = "$projectDir/../../backend.native/tests/samples/videoplayer"

val linkerOpts = when {
    PlatformInfo.isMac() -> listOf("-linker-options", "-L/opt/local/lib", "-linker-options", "-L/usr/local/lib", "-linker-options", "-L/opt/homebrew/lib", "-linker-options", "-L/opt/homebrew/opt/ffmpeg@4/lib")
    PlatformInfo.isLinux() -> listOf("-linker-options", "-L/usr/lib/x86_64-linux-gnu", "-linker-options", "-L/usr/lib64")
    PlatformInfo.isWindows() -> listOf("-linker-options", "-L$mingwPath/lib")
    else -> error("Unsupported platform")
}

var includeDirsFfmpeg = emptyList<String>()
var filterDirsFfmpeg = emptyList<String>()
when {
    PlatformInfo.isMac() -> filterDirsFfmpeg = listOf(
        "-headerFilterAdditionalSearchPrefix", "/opt/local/include",
        "-headerFilterAdditionalSearchPrefix", "/usr/local/include",
        "-headerFilterAdditionalSearchPrefix", "/opt/homebrew/opt/ffmpeg@4/include"
    )
    PlatformInfo.isLinux() -> filterDirsFfmpeg = listOf(
        "-headerFilterAdditionalSearchPrefix", "/usr/include",
        "-headerFilterAdditionalSearchPrefix", "/usr/include/x86_64-linux-gnu",
        "-headerFilterAdditionalSearchPrefix", "/usr/include/ffmpeg"
    )
    PlatformInfo.isWindows() -> includeDirsFfmpeg = listOf("-compiler-option", "-I$mingwPath/include")
}

var includeDirsSdl = when {
    PlatformInfo.isMac() -> listOf(
        "-compiler-option", "-I/opt/local/include/SDL2",
        "-compiler-option", "-I/usr/local/include/SDL2",
        "-compiler-option", "-I/opt/homebrew/include/SDL2"
    )
    PlatformInfo.isLinux() -> listOf("-compiler-option", "-I/usr/include/SDL2")
    PlatformInfo.isWindows() -> listOf("-compiler-option", "-I$mingwPath/include/SDL2")
    else -> error("Unsupported platform")
}

val defaultCompilerOpts =  listOf("-g")
val buildOpts = getCompileOnlyBenchmarksOpts(project, defaultCompilerOpts)

compileBenchmark {
    applicationName = "Videoplayer"
    repeatNumber = 10
    compilerOpts = buildOpts
    buildSteps {
        step("runCinteropFfmpeg") {
            command = listOf(
                "$dist/bin/cinterop$toolSuffix",
                "-o", "$videoplayerDir/build/classes/kotlin/videoPlayer/main/videoplayer-cinterop-ffmpeg.klib",
                "-def", "$videoplayerDir/src/nativeInterop/cinterop/ffmpeg.def"
            ) + filterDirsFfmpeg + includeDirsFfmpeg
        }
        step("runCinteropSdl") {
            command = listOf(
                "$dist/bin/cinterop$toolSuffix",
                "-o", "$videoplayerDir/build/classes/kotlin/videoPlayer/main/videoplayer-cinterop-sdl.klib",
                "-def", "$videoplayerDir/src/nativeInterop/cinterop/sdl.def"
            ) + includeDirsSdl
        }
        step("runKonanProgram") {
            command = listOf(
                "$dist/bin/konanc$toolSuffix",
                "-ea", "-p", "program",
                "-o", layout.buildDirectory.file("program$binarySuffix").get().asFile.toString(),
                "-l", "$videoplayerDir/build/classes/kotlin/videoPlayer/main/videoplayer-cinterop-ffmpeg.klib",
                "-l", "$videoplayerDir/build/classes/kotlin/videoPlayer/main/videoplayer-cinterop-sdl.klib",
                "-Xmulti-platform", "$videoplayerDir/src/videoPlayerMain/kotlin",
                "-entry", "sample.videoplayer.main"
            ) + buildOpts + linkerOpts
        }
    }
}
