/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("swift-benchmarking")
}

val toolsPath = "../../tools"
val targetExtension = "Macos"

swiftBenchmark {
    applicationName = "swiftInterop"
    commonSrcDirs = listOf("$toolsPath/benchmarks/shared/src/main/kotlin", "src", "../shared/src/main/kotlin")
    nativeSrcDirs = listOf("../shared/src/main/kotlin-native/common", "../shared/src/main/kotlin-native/posix")
    swiftSources = listOf("$projectDir/swiftSrc/benchmarks.swift", "$projectDir/swiftSrc/main.swift")
    compileTasks = listOf("compileKotlinNative", "linkBenchmarkReleaseFrameworkNative")

    dependencies.common(project(":endorsedLibraries:kotlinx.cli"))
}