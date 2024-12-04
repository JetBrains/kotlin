/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled

internal val KotlinLegacyMetadataArtifact = KotlinTargetArtifact artifact@{ target, apiElements, _ ->
    if (target !is KotlinMetadataTarget) return@artifact
    if (target.project.isKotlinGranularMetadataEnabled) return@artifact

    val metadataJar = target.createArtifactsTask { jar ->
        jar.from(target.compilations.getByName(MAIN_COMPILATION_NAME).output.allOutputs)
    }

    target.createPublishArtifact(metadataJar, JAR_TYPE, apiElements)
}
