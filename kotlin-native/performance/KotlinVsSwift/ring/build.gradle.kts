/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.benchmark.CodeSizeEntity

plugins {
    id("swift-benchmarking")
}

val toolsPath = "../../../tools"

swiftBenchmark {
    applicationName = "Ring"
    commonSrcDirs = listOf("$toolsPath/benchmarks/shared/src/main/kotlin", "../../shared/src/main/kotlin")
    nativeSrcDirs = listOf("../../shared/src/main/kotlin-native/common", "../../shared/src/main/kotlin-native/posix")
    swiftSources = File("$projectDir/src").list().map { "$projectDir/src/$it" }.toList()
    compileTasks = listOf("buildSwift")
    useCodeSize = CodeSizeEntity.EXECUTABLE
}