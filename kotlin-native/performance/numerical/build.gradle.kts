/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.benchmark.BenchmarkingPlugin
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.HostManager

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
    linkerOpts = listOf("$buildDir/pi.o")
    buildType = (findProperty("nativeBuildType") as String?)?.let { NativeBuildType.valueOf(it) } ?: defaultBuildType

    dependencies.common(project(":endorsedLibraries:kotlinx.cli"))
}

val compileLibary by tasks.creating {
    doFirst {
        mkdir(buildDir)

        project.withConvention(ExecClang::class) {
            execKonanClang(HostManager.host) {
                args("-O3")
                args("-c", "$projectDir/src/nativeInterop/cinterop/pi.c")
                args("-o", "$buildDir/pi.o")
            }
        }
    }
}

val native = kotlin.targets.getByName("native") as KotlinNativeTarget
native.apply {
    compilations["main"].cinterops {
        create("cinterop") {
            headers("$projectDir/src/nativeInterop/cinterop/pi.h")
        }
    }
    binaries.getExecutable(BenchmarkingPlugin.NATIVE_EXECUTABLE_NAME, "RELEASE").linkTask.dependsOn(compileLibary)
}
