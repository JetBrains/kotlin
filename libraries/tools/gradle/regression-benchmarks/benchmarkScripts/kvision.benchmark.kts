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
    gitCommitSha = "c1ca778346cdd1f47b2a0d6c9f07878dae215c96",
    stableKotlinVersion = "1.8.21",
)

import java.io.File

val repoPatch = {
    "kvision-kotlin-current.patch" to File("benchmarkScripts/files/kvision-kotlin-repo.patch")
        .readText()
        .run { replace("<kotlin_version>", currentKotlinVersion) }
        .byteInputStream()
}

runBenchmarks(
    repoPatch,
    suite {
        scenario {
            title = "Build Js clean build"

            runTasks("jsJar")
            runCleanupTasks("clean")
        }

        scenario {
            title = "Build Js IR with ABI change in ObservableList"

            runTasks("jsJar")
            applyAbiChangeTo("kvision-modules/kvision-state/src/main/kotlin/io/kvision/state/ObservableList.kt")
        }

        scenario {
            title = "Build Js IR with non-ABI change in ObservableList"

            runTasks("jsJar")
            applyNonAbiChangeTo("kvision-modules/kvision-state/src/main/kotlin/io/kvision/state/ObservableList.kt")
        }

        scenario {
            title = "Dry run configuration time"
            useGradleArgs("-m")

            runTasks("jsJar")
        }

        scenario {
            title = "No-op configuration time"

            runTasks("help")
        }

        scenario {
            title = "UP-TO-DATE configuration time"

            runTasks("jsJar")
        }
    }
)