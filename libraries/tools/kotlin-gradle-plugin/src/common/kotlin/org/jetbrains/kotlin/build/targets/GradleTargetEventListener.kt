/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.targets

import org.gradle.api.Project
import org.jetbrains.kotlin.build.KotlinMultiplatformProjectEventListener
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset

internal data class GradleTargetCreationContext(
    val targetPreset: InternalKotlinTargetPreset<*>
)

internal class GradleTargetEventListener(
    private val project: Project,
    private val kotlinMultiplatformExtension: KotlinMultiplatformExtension
) : TargetEventsListener, KotlinMultiplatformProjectEventListener {
    override fun onTargetCreated(kotlinTarget: KotlinTarget, context: Any?) {
        require(context is GradleTargetCreationContext) { "Unexpected context $context" }

        val targetName = kotlinTarget.id
        val targetPreset = context.targetPreset
        val newTarget = targetPreset.createTargetInternal(targetName)
        kotlinMultiplatformExtension.targets.add(newTarget)
    }

    override fun onConfigurationStart() {
        val metadataPreset = KotlinMetadataTargetPreset(project)
        val metadataTarget = metadataPreset.createTargetInternal(KotlinMultiplatformPlugin.METADATA_TARGET_NAME)
        kotlinMultiplatformExtension.targets.add(metadataTarget)
    }
}
