/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.konan.target.*

plugins {
    id("swift-benchmarking")
}

kotlin {
    applyDefaultHierarchyTemplate() // due to custom posixMain source set

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
            }
            kotlin.srcDir("src/main/kotlin")
            kotlin.srcDir("../reports/src/main/kotlin/report")
            kotlin.srcDir("../shared/src/main/kotlin")
        }
        nativeMain {
            kotlin.srcDir("src/main/kotlin-native")
            kotlin.srcDir("../shared/src/main/kotlin-native/common")
        }
        mingwMain {
            kotlin.srcDir("../shared/src/main/kotlin-native/mingw")
        }
        val posixMain by creating {
            dependsOn(nativeMain.get())
            kotlin.srcDir("../shared/src/main/kotlin-native/posix")
        }
        linuxMain.get().dependsOn(posixMain)
        appleMain.get().dependsOn(posixMain)
    }
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
    compileTasks = listOf("compileKotlinNative", "linkBenchmark${buildType.name.toLowerCase().capitalize()}FrameworkNative")
}
