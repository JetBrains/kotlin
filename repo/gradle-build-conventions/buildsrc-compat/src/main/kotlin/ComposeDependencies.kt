import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import java.net.URI

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
private val Project.composeSnapshotVersionCatalog: VersionCatalog
    get() = project.extensions.getByType(VersionCatalogsExtension::class.java).find("composeRuntimeSnapshot").get()
private val Project.libsVersionCatalog: VersionCatalog
    get() = project.extensions.getByType(VersionCatalogsExtension::class.java).find("libs").get()
private fun Project.composeStableVersion() = libsVersionCatalog.findVersion("compose.stable").get().requiredVersion
private fun Project.composeSnapshotVersion() = composeSnapshotVersionCatalog.findVersion("snapshot.version").get().requiredVersion

val Project.androidXMavenLocalPath: String?
    get() = kotlinBuildProperties.getOrNull("compose.aosp.root")?.toString()

fun RepositoryHandler.androidXMavenLocal(androidXMavenLocalPath: String?) {
    if (androidXMavenLocalPath != null) {
        maven("$androidXMavenLocalPath/out/dist/repository/")
    }
}

fun RepositoryHandler.androidxSnapshotRepo(composeSnapshotId: String) {
    maven {
        url = URI("https://androidx.dev/snapshots/builds/${composeSnapshotId}/artifacts/repository")
    }.apply {
        content {
            includeGroup("androidx.compose.runtime")
            includeGroup("androidx.collection")
            includeGroup("androidx.annotation")
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
