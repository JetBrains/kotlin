import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.provider.Provider
import java.net.URI

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
private val Project.versionCatalog: VersionCatalog
    get() = project.extensions.getByType(VersionCatalogsExtension::class.java).find("libs").get()
private fun Project.composeStableVersion() = versionCatalog.findVersion("compose.stable").get().requiredVersion
private fun Project.composeSnapshotVersion() = versionCatalog.findVersion("compose.snapshot.version").get().requiredVersion
private fun Project.composeSnapshotId() = versionCatalog.findVersion("compose.snapshot.id").get().requiredVersion

fun RepositoryHandler.androidxSnapshotRepo(composeSnapshotVersion: String) {
    maven {
        url = URI("https://androidx.dev/snapshots/builds/${composeSnapshotVersion}/artifacts/repository")
    }.apply {
        content {
            includeGroup("androidx.compose.runtime")
        }
    }
}

fun RepositoryHandler.composeGoogleMaven(composeStableVersion: String) {
    google {
        content {
            includeGroup("androidx.collection")
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

fun Project.composeRuntime() = compose("runtime", "runtime", composeSnapshotVersion())
fun Project.composeRuntimeTestUtils() = compose("runtime", "runtime-test-utils", composeSnapshotVersion())
fun Project.compose(group: String, module: String, version: String = composeStableVersion()) =
    "androidx.compose.$group:$module:$version"