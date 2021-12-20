// Android application written in Kotlin:
// - kind of big codebase
// - uses Kotlin compiler plugins and kapt

@file:BenchmarkProject(
    name = "duckduckgo",
    gitUrl = "https://github.com/duckduckgo/Android.git",
    gitCommitSha = "659bd5aaac1fba922a6df9053daa2b7bcd610375"
)

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
        "1.6.10" to "benchmarkScripts/files/duckduckgo-kotlin-1.6.10.patch",
        "1.6.20" to "benchmarkScripts/files/duckduckgo-kotlin-current.patch"
    )
)
