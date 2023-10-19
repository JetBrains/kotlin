/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.metadata.isCompatibilityMetadataVariantEnabled
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.tasks.registerTask

internal val KotlinLegacyCompatibilityMetadataArtifact = KotlinTargetArtifact { target, apiElements, _ ->
    if (target !is KotlinMetadataTarget) return@KotlinTargetArtifact
    if (!target.project.isKotlinGranularMetadataEnabled) return@KotlinTargetArtifact
    if (!target.project.isCompatibilityMetadataVariantEnabled) return@KotlinTargetArtifact

    val legacyJar = target.project.registerTask<Jar>(target.legacyArtifactsTaskName)
    legacyJar.configure {
        // Capture it here to use in onlyIf spec. Direct usage causes serialization of target attempt when configuration cache is enabled
        it.description = "Assembles an archive containing the Kotlin metadata of the commonMain source set."
        it.from(target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).output.allOutputs)
    }

    target.createPublishArtifact(legacyJar, JAR_TYPE, apiElements)
}