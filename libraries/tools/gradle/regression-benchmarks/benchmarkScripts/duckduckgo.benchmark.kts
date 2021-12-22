// Android application written in Kotlin:
// - kind of big codebase
// - uses Kotlin compiler plugins and kapt
@file:BenchmarkProject(
    name = "duckduckgo",
    gitUrl = "https://github.com/duckduckgo/Android.git",
    gitCommitSha = "659bd5aaac1fba922a6df9053daa2b7bcd610375"
)

import java.io.File

val stableReleasePatch = {
    "duckduckgo-kotlin-1.6.10.patch" to File("benchmarkScripts/files/duckduckgo-kotlin-1.6.10.patch").inputStream()
}

val currentReleasePatch = {
    "duckduckgo-kotlin-current.patch" to File("benchmarkScripts/files/duckduckgo-kotlin-current.patch")
        .readText()
        .run { replace("<kotlin_version>", currentKotlinVersion) }
        .byteInputStream()
}

runAllBenchmarks(
    suite {
        scenario {
            title = "Clean build"
            warmups = 3
            iterations = 7
            useGradleArgs("--no-build-cache")

            runTasks("assembleDebug")
            runCleanupTasks("clean")
        }

        scenario {
            title = "Incremental build with ABI change"
            warmups = 3
            iterations = 7
            useGradleArgs("--no-build-cache")

            runTasks("assembleDebug")
            applyAbiChangeTo("common/src/main/java/com/duckduckgo/app/global/ViewModelFactory.kt")
        }
    },
    mapOf(
        "1.6.10" to stableReleasePatch,
        "1.6.20" to currentReleasePatch
    )
)
