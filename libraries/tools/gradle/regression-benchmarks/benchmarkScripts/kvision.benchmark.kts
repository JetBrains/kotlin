/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Pure 'org.jetbrains.kotlin.js' project (without MPP)
// - relatively big codebase
// - usually updated to the latest Kotlin version
@file:BenchmarkProject(
    name = "kvision",
    gitUrl = "https://github.com/rjaros/kvision.git",
    gitCommitSha = "9204d7be944a57bd6c09053bc16b5eeea1bccac5"
)

import java.io.File

val currentReleasePatch = {
    "kvision-kotlin-current.patch" to File("benchmarkScripts/files/kvision-kotlin-current.patch")
        .readText()
        .run { replace("<kotlin_version>", currentKotlinVersion) }
        .byteInputStream()
}

runAllBenchmarks(
    suite {
        // Disabled due to the possible error "No space left on device"
        // Kotlin/Js/Ir trashes /tmp directory with files that are not reused
        // Check https://youtrack.jetbrains.com/issue/KT-52176/Kotlin-JS-Ir-compilation-creates-build-files-in-system-temp-dire
//        scenario {
//            title = "Build Js IR clean build"
//
//            runTasks("jsIrJar")
//            runCleanupTasks("clean")
//        }

        scenario {
            title = "Build Js Legacy clean build"

            runTasks("jsLegacyJar")
            runCleanupTasks("clean")
        }

        scenario {
            title = "Build Js Legacy with ABI change in ObservableList"

            runTasks("jsLegacyJar")
            applyAbiChangeTo("kvision-modules/kvision-state/src/main/kotlin/io/kvision/state/ObservableList.kt")
        }

        scenario {
            title = "Build Js Legacy with non-ABI change in ObservableList"

            runTasks("jsLegacyJar")
            applyNonAbiChangeTo("kvision-modules/kvision-state/src/main/kotlin/io/kvision/state/ObservableList.kt")
        }
    },
    mapOf(
        "1.6.20" to null,
        "1.7.0" to  currentReleasePatch
    )
)