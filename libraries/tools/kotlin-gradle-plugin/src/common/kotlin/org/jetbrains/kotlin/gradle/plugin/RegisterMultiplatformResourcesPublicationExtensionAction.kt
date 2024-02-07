/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetWithPublishableMultiplatformResources
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetWithResolvableMultiplatformResources
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesResolutionExtension


internal val RegisterMultiplatformResourcesPublicationExtensionAction = KotlinProjectSetupAction {
    project.launch {
        project.multiplatformExtension.awaitTargets()
            .forEach { target ->
                if (target is KotlinTargetWithPublishableMultiplatformResources) {
                    target.resourcesPublicationExtension
                    if (target is KotlinTargetWithResolvableMultiplatformResources) {
                        target.resourcesResolutionExtension
                    }
                }
            }
    }
}