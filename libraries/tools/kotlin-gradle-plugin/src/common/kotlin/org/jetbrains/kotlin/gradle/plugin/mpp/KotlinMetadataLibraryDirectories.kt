/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import java.io.File

private const val kotlinTransformedMetadataLibraries = "kotlinTransformedMetadataLibraries"
private const val kotlinTransformedCInteropMetadataLibraries = "kotlinTransformedCInteropMetadataLibraries"

internal fun Project.kotlinTransformedMetadataLibraryDirectoryForBuild(sourceSetName: String): File =
    buildDir.resolve(kotlinTransformedMetadataLibraries).resolve(sourceSetName)

internal val Project.kotlinTransformedMetadataLibraryDirectoryForIde: File
    get() = rootDir.resolve(".gradle").resolve("kotlin").resolve(kotlinTransformedMetadataLibraries)

internal fun Project.kotlinTransformedCInteropMetadataLibraryDirectoryForBuild(sourceSetName: String): File =
    buildDir.resolve(kotlinTransformedCInteropMetadataLibraries).resolve(sourceSetName)

internal val Project.kotlinTransformedCInteropMetadataLibraryDirectoryForIde: File
    get() = rootDir.resolve(".gradle").resolve("kotlin").resolve(kotlinTransformedCInteropMetadataLibraries)
