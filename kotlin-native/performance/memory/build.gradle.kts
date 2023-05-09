import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.RunKotlinNativeTask
import org.jetbrains.kotlin.BenchmarkRepeatingType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.benchmark.BenchmarkingPlugin

/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("benchmarking")
}

benchmark {
    applicationName = "Memory manager"

    val launcherSources = listOf("src/main/kotlin")

    commonSrcDirs = listOf("../../tools/benchmarks/shared/src/main/kotlin/report", "../shared/src/main/kotlin") + launcherSources
    nativeSrcDirs = listOf("../shared/src/main/kotlin-native/common")

    jvmSrcDirs = listOf("../shared/src/main/kotlin-jvm")
    mingwSrcDirs = listOf("../shared/src/main/kotlin-native/mingw")
    posixSrcDirs = listOf("../shared/src/main/kotlin-native/posix")

    repeatingType = BenchmarkRepeatingType.EXTERNAL
    warmupCount = 1
    repeatCount = 5

//    val runTask = tasks.get("konanRun") as RunKotlinNativeTask
//
//    val nativeTarget = kotlin.targets.getByName(BenchmarkingPlugin.NATIVE_TARGET_NAME) as KotlinNativeTarget
//    val nativeBinary = nativeTarget.binaries.getExecutable(BenchmarkingPlugin.NATIVE_EXECUTABLE_NAME, benchmark.buildType)
//    val nativeExecutable = nativeBinary.outputFile.absolutePath
//
//    runTask.args("launcher", nativeExecutable)
}
