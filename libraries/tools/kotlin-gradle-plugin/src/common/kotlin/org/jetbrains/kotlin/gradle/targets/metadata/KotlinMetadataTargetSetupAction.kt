/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.metadata

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.targetFromPresetInternal
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTargetPreset

internal val KotlinMetadataTargetSetupAction = KotlinProjectSetupAction {
    /* Create metadata target */
    multiplatformExtension.targetFromPresetInternal(
        KotlinMetadataTargetPreset(project),
        KotlinMetadataTarget.METADATA_TARGET_NAME
    )
}
