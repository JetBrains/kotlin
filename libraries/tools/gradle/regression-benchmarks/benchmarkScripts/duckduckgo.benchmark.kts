// Android application written in Kotlin:
// - kind of big codebase
// - uses Kotlin compiler plugins and kapt
@file:BenchmarkProject(
    name = "duckduckgo",
    gitUrl = "https://github.com/duckduckgo/Android.git",
    gitCommitSha = "648b0aae2dd54c4da4176eb91b3a05ea44118fa5"
)

import java.io.File

val stableReleasePatch = {
    "duckduckgo-kotlin-1.7.10.patch" to File("benchmarkScripts/files/duckduckgo-kotlin-1.7.10.patch")
        .readText()
        .byteInputStream()
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

            runTasks(":app:assemblePlayDebug")
            runCleanupTasks("clean")
        }

        scenario {
            title = "Incremental build with ABI change in common ViewModelFactory"
            useGradleArgs("--no-build-cache")

            runTasks(":app:assemblePlayDebug")
            applyAbiChangeTo("common/src/main/java/com/duckduckgo/app/global/VpnViewModelFactory.kt")
        }

        scenario {
            title = "Incremetal build with ABI change in Kapt component"
            useGradleArgs("--no-build-cache")

            runTasks(":app:assemblePlayDebug")
            applyAbiChangeTo("vpn/src/main/java/com/duckduckgo/mobile/android/vpn/di/VpnModule.kt")
        }

        scenario {
            title = "Incremental build with change in Android common string resource"
            useGradleArgs("--no-build-cache")

            runTasks(":app:assemblePlayDebug")

            applyAndroidResourceValueChange("common-ui/src/main/res/values/strings.xml")
        }
    },
    mapOf(
        "1.7.10" to stableReleasePatch,
        "1.7.20" to currentReleasePatch
    )
)
