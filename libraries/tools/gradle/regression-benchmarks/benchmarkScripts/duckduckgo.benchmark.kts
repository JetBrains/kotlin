// Android application written in Kotlin:
// - kind of big codebase
// - uses Kotlin compiler plugins and kapt
@file:BenchmarkProject(
    name = "duckduckgo",
    gitUrl = "https://github.com/duckduckgo/Android.git",
    gitCommitSha = "5bec6f290634f1e9eb8f7956099de5384c604423",
    stableKotlinVersion = "2.0.20",
)

import java.io.File

val repoPatch = {
    listOf(
        "duckduckgo-kotlin-repo.patch" to File("benchmarkScripts/files/duckduckgo-kotlin-repo.patch")
            .readText()
            .run { replace("<kotlin_version>", currentKotlinVersion) }
            .byteInputStream(),
    )
}

runBenchmarks(
    repoPatch,
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
            applyAbiChangeTo("common/common-utils/src/main/java/com/duckduckgo/app/global/VpnViewModelFactory.kt")
        }

        scenario {
            title = "Incremental build with ABI change in Kapt component"
            useGradleArgs("--no-build-cache")

            runTasks(":app:assemblePlayDebug")
            applyAbiChangeTo("app-tracking-protection/vpn-impl/src/main/java/com/duckduckgo/mobile/android/vpn/di/VpnModule.kt")
        }

        scenario {
            title = "Incremental build with non ABI change in Kapt component"
            useGradleArgs("--no-build-cache")

            runTasks(":app:assemblePlayDebug")
            applyNonAbiChangeTo("app-tracking-protection/vpn-impl/src/main/java/com/duckduckgo/mobile/android/vpn/di/VpnModule.kt")
        }

        scenario {
            title = "Incremental build with change in Android common string resource"
            useGradleArgs("--no-build-cache")

            runTasks(":app:assemblePlayDebug")

            applyAndroidResourceValueChange("common/common-utils/src/main/res/values/strings-common.xml")
        }

        scenario {
            title = "Dry run configuration time"
            useGradleArgs("-m")

            runTasks(":app:assemblePlayDebug")
        }

        scenario {
            title = "No-op configuration time"

            runTasks("help")
        }

        scenario {
            title = "UP-TO-DATE configuration time"

            runTasks(":app:assemblePlayDebug")
        }
    }
)
