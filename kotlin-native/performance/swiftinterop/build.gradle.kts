/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.konan.target.*

plugins {
    id("swift-benchmarking")
}

kotlin {
    macosArm64()
}

val konanDataDir = if (project.hasProperty("konan.data.dir")) project.property("konan.data.dir").toString() else null
project.extra["platformManager"] = PlatformManager(buildDistribution(projectDir.parentFile.parentFile.absolutePath, konanDataDir), false)
swiftBenchmark {
    applicationName = "swiftInterop"
    swiftSources = listOf(
        "$projectDir/swiftSrc/benchmarks.swift",
        "$projectDir/swiftSrc/main.swift",
        "$projectDir/swiftSrc/weakRefBenchmarks.swift",
    )
}
