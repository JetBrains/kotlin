import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("benchmarking")
}

benchmark {
    applicationName = "Logging"
    commonSrcDirs = listOf("../../tools/benchmarks/shared/src/main/kotlin/report", "src/main/kotlin", "../shared/src/main/kotlin")
    jvmSrcDirs = listOf("../shared/src/main/kotlin-jvm")
    nativeSrcDirs = listOf("../shared/src/main/kotlin-native/common")
    mingwSrcDirs = listOf("../shared/src/main/kotlin-native/mingw")
    posixSrcDirs = listOf("../shared/src/main/kotlin-native/posix")
    compilerOpts = listOf("-Xruntime-logs=logging=debug")
}
