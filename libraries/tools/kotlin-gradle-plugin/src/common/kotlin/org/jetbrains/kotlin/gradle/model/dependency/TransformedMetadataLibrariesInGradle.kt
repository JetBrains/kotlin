/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.dependency

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.build.TransformedMetadataLibrariesModel
import org.jetbrains.kotlin.gradle.model.KotlinMetadataLibrariesInGradle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.retrieveExternalDependencies
import org.jetbrains.kotlin.gradle.utils.future

class TransformedMetadataLibrariesInGradle(
    private val sourceSet: KotlinSourceSet,
    private val transitive: Boolean,
) : TransformedMetadataLibrariesModel, KotlinMetadataLibrariesInGradle {
    override val files: FileCollection by lazy {
        sourceSet.project.future { sourceSet.retrieveExternalDependencies(transitive) }.getOrThrow()
    }
}