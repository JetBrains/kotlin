import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.net.URI

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

private const val composeSnapshotId = "11751492"
private const val composeVersion = "1.7.0-SNAPSHOT"

fun RepositoryHandler.androidxSnapshotRepo() {
    maven {
        url = URI("https://androidx.dev/snapshots/builds/$composeSnapshotId/artifacts/repository")
    }
}

fun Project.composeRuntime() = "androidx.compose.runtime:runtime:${composeVersion}"
fun Project.composeRuntimeTestUtils() = "androidx.compose.runtime:runtime-test-utils:${composeVersion}"
fun Project.composeUi() = "androidx.compose.ui:ui:${composeVersion}"
fun Project.composeFoundation() = "androidx.compose.foundation:foundation:${composeVersion}"