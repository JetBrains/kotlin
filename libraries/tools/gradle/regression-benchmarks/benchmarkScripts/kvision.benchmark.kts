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
    gitCommitSha = "3fe69bf6db9a3650b026630d857862f3cee6485b"
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
        scenario {
            title = "Build Js IR clean build"

            runTasks("jsIrJar")
            runCleanupTasks("clean")
        }

        scenario {
            title = "Build Js IR with ABI change in ObservableList"

            runTasks("jsIrJar")
            applyAbiChangeTo("kvision-modules/kvision-state/src/main/kotlin/io/kvision/state/ObservableList.kt")
        }

        scenario {
            title = "Build Js IR with non-ABI change in ObservableList"

            runTasks("jsIrJar")
            applyNonAbiChangeTo("kvision-modules/kvision-state/src/main/kotlin/io/kvision/state/ObservableList.kt")
        }

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

        scenario {
            title = "Dry run configuration time"
            useGradleArgs("-m")

            iterations = 20
            runTasks("jsIrJar")
        }

        scenario {
            title = "No-op configuration time"

            iterations = 20
            runTasks("help")
        }

        scenario {
            title = "UP-TO-DATE configuration time"

            iterations = 20
            runTasks("jsIrJar")
        }
    },
    mapOf(
        "1.7.20" to null,
        "1.8.0" to currentReleasePatch
    )
)