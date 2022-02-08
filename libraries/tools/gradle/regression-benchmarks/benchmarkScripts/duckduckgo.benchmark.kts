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
            useGradleArgs("--no-build-cache")

            runTasks("assembleDebug")
            runCleanupTasks("clean")
        }

        scenario {
            title = "Incremental build with ABI change in common ViewModelFactory"
            useGradleArgs("--no-build-cache")

            runTasks("assembleDebug")
            applyAbiChangeTo("common/src/main/java/com/duckduckgo/app/global/ViewModelFactory.kt")
        }

        scenario {
            title = "Incremetal build with ABI change in Kapt component"
            useGradleArgs("--no-build-cache")

            runTasks("assembleDebug")
            applyAbiChangeTo("vpn-main/src/main/java/com/duckduckgo/mobile/android/vpn/di/VpnComponent.kt")
        }

        scenario {
            title = "Incremental build with change in Android common string resource"
            useGradleArgs("--no-build-cache")

            runTasks("assembleDebug")

            applyAndroidResourceValueChange("common-ui/src/main/res/values/strings.xml")
        }
    },
    mapOf(
        "1.6.10" to stableReleasePatch,
        "1.6.20" to currentReleasePatch
    )
)
