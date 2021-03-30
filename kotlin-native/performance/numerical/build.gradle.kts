/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.benchmark.BenchmarkingPlugin
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.*

plugins {
    id("benchmarking")
}

val defaultBuildType = NativeBuildType.RELEASE

benchmark {
    applicationName = "Numerical"
    commonSrcDirs = listOf("src/main/kotlin", "../../tools/benchmarks/shared/src/main/kotlin/report", "../shared/src/main/kotlin")
    jvmSrcDirs = listOf("src/main/kotlin-jvm", "../shared/src/main/kotlin-jvm")
    nativeSrcDirs = listOf("src/main/kotlin-native", "../shared/src/main/kotlin-native/common")
    mingwSrcDirs = listOf("../shared/src/main/kotlin-native/mingw")
    posixSrcDirs = listOf("../shared/src/main/kotlin-native/posix")
    buildType = (findProperty("nativeBuildType") as String?)?.let { NativeBuildType.valueOf(it) } ?: defaultBuildType
}

val native = kotlin.targets.getByName("native") as KotlinNativeTarget
native.apply {
    compilations["main"].cinterops {
        create("cinterop") {
            headers("$projectDir/src/nativeInterop/cinterop/pi.h")
            extraOpts("-Xcompile-source", "$projectDir/src/nativeInterop/cinterop/pi.c")
            extraOpts("-Xsource-compiler-option", "-O3")
        }
    }
}
