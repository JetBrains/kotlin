import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

private const val composeSnapshotId = "11809876"
private const val composeSnapshotVersion = "1.7.0-SNAPSHOT"
val composeStableVersion = "1.7.0-alpha07"

fun RepositoryHandler.androidxSnapshotRepo() {
    maven {
        url = URI("https://androidx.dev/snapshots/builds/$composeSnapshotId/artifacts/repository")
    }.apply {
        content {
            includeGroup("androidx.compose.runtime")
        }
    }
}

fun RepositoryHandler.composeGoogleMaven() {
    google {
        content {
            includeGroup("androidx.collection")
            includeVersion("androidx.compose.runtime", "runtime", composeStableVersion)
            includeVersion("androidx.compose.runtime", "runtime-desktop", composeStableVersion)
            includeVersion("androidx.compose.foundation", "foundation-layout", composeStableVersion)
            includeVersion("androidx.compose.foundation", "foundation-layout-desktop", composeStableVersion)
            includeVersion("androidx.compose.foundation", "foundation", composeStableVersion)
            includeVersion("androidx.compose.foundation", "foundation-desktop", composeStableVersion)
            includeVersion("androidx.compose.animation", "animation", composeStableVersion)
            includeVersion("androidx.compose.animation", "animation-desktop", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-desktop", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-graphics", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-graphics-desktop", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-text", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-text-desktop", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-unit", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-unit-desktop", composeStableVersion)
        }
    }
}

fun Project.composeRuntime() = "androidx.compose.runtime:runtime:${composeSnapshotVersion}"
fun Project.composeRuntimeTestUtils() = "androidx.compose.runtime:runtime-test-utils:${composeSnapshotVersion}"