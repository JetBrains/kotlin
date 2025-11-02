/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.compilation

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.build.KotlinMetadataCompilationOutputModel
import org.jetbrains.kotlin.gradle.model.KotlinMetadataLibrariesInGradle
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet

internal class KotlinMetadataCompilationOutputInGradle(
    private val metadataTarget: KotlinMetadataTarget,
    private val sourceSet: InternalKotlinSourceSet,
) : KotlinMetadataCompilationOutputModel, KotlinMetadataLibrariesInGradle {
    override val files: FileCollection
        get() {
            val metadataCompilation = metadataTarget.compilations.findByName(sourceSet.name)
                ?: error("Metadata compilation not found for source set: ${sourceSet.name}")

            return metadataCompilation.output.classesDirs
        }

}