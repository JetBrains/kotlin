// Android application written in Kotlin:
// - kind of big codebase
// - uses Kotlin compiler plugins and kapt
@file:BenchmarkProject(
    name = "duckduckgo",
    gitUrl = "https://github.com/duckduckgo/Android.git",
    gitCommitSha = "18e230fcefbefb4317c1fe128b4539a2315e7c0a"
)

import java.io.File

val stableReleasePatch = {
    "duckduckgo-kotlin-1.7.20.patch" to File("benchmarkScripts/files/duckduckgo-kotlin-1.7.20.patch")
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
            title = "Incremental build with ABI change in Kapt component"
            useGradleArgs("--no-build-cache")

            runTasks(":app:assemblePlayDebug")
            applyAbiChangeTo("app-tracking-protection/vpn-impl/src/main/java/com/duckduckgo/mobile/android/vpn/network/VpnNetworkModule.kt")
        }

        scenario {
            title = "Incremental build with ABI change in Kapt component"
            useGradleArgs("--no-build-cache")

            runTasks(":app:assemblePlayDebug")
            applyNonAbiChangeTo("app-tracking-protection/vpn-impl/src/main/java/com/duckduckgo/mobile/android/vpn/network/VpnNetworkModule.kt")
        }

        scenario {
            title = "Incremental build with change in Android common string resource"
            useGradleArgs("--no-build-cache")

            runTasks(":app:assemblePlayDebug")

            applyAndroidResourceValueChange("common-ui/src/main/res/values/strings.xml")
        }

        scenario {
            title = "Dry run configuration time"
            useGradleArgs("-m")

            iterations = 20
            runTasks(":app:assemblePlayDebug")
        }

        scenario {
            title = "No-op configuration time"

            iterations = 20
            runTasks("help")
        }

        scenario {
            title = "UP-TO-DATE configuration time"

            iterations = 20
            runTasks(":app:assemblePlayDebug")
        }
    },
    mapOf(
        "1.7.20" to stableReleasePatch,
        "1.8.0" to currentReleasePatch
    )
)
