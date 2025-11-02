/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

internal interface KotlinLibrariesInGradle {
    val files: FileCollection
}

internal interface KotlinMetadataLibrariesInGradle : KotlinLibrariesInGradle
internal interface KotlinPlatformLibrariesInGradle : KotlinLibrariesInGradle

internal fun Project.kotlinLibrariesInGradleToFileCollection(libraries: List<KotlinLibrariesInGradle>): FileCollection {
    return files(libraries.map { it.files })
}