/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.jetbrains.kotlin.gradle.utils.compositeBuildRootProject
import org.jetbrains.kotlin.gradle.utils.kotlinMetadataDir
import java.io.File
import java.nio.file.Path

private const val kotlinTransformedMetadataLibraries = "kotlinTransformedMetadataLibraries"
private const val kotlinCInteropLibraries = "kotlinCInteropLibraries"
private const val kotlinTransformedCInteropMetadataLibraries = "kotlinTransformedCInteropMetadataLibraries"

internal fun ProjectLayout.kotlinTransformedMetadataLibraryDirectoryForBuild(sourceSetName: String): File =
    kotlinTransformedMetadataLibraryDirectoryPathForBuild(sourceSetName).toFile()

internal fun ProjectLayout.kotlinTransformedMetadataLibraryDirectoryPathForBuild(sourceSetName: String): Path =
    buildDirectory.get().asFile.toPath().resolve(kotlinTransformedMetadataLibraries).resolve(sourceSetName)

internal val Project.kotlinTransformedMetadataLibraryDirectoryForIde: File
    get() = kotlinTransformedMetadataLibraryDirectoryPathForIde.toFile()

internal val Project.kotlinTransformedMetadataLibraryDirectoryPathForIde: Path
    get() = kotlinMetadataDir(compositeBuildRootProject).toPath()
        .resolve(kotlinTransformedMetadataLibraries)

internal fun ProjectLayout.kotlinTransformedCInteropMetadataLibraryDirectoryForBuild(sourceSetName: String): File =
    kotlinTransformedCInteropMetadataLibraryDirectoryPathForBuild(sourceSetName).toFile()

internal fun ProjectLayout.kotlinTransformedCInteropMetadataLibraryDirectoryPathForBuild(sourceSetName: String): Path =
    buildDirectory.get().asFile.toPath().resolve(kotlinTransformedCInteropMetadataLibraries).resolve(sourceSetName)

internal val Project.kotlinCInteropLibraryDirectoryForIde: File
    get() = kotlinCInteropLibraryDirectoryPathForIde.toFile()

internal val Project.kotlinCInteropLibraryDirectoryPathForIde: Path
    get() = kotlinMetadataDir(compositeBuildRootProject).toPath()
        .resolve(kotlinCInteropLibraries)

internal val Project.kotlinTransformedCInteropMetadataLibraryDirectoryForIde: File
    get() = kotlinTransformedCInteropMetadataLibraryDirectoryPathForIde.toFile()

internal val Project.kotlinTransformedCInteropMetadataLibraryDirectoryPathForIde: Path
    get() = kotlinMetadataDir(compositeBuildRootProject).toPath()
        .resolve(kotlinTransformedCInteropMetadataLibraries)
