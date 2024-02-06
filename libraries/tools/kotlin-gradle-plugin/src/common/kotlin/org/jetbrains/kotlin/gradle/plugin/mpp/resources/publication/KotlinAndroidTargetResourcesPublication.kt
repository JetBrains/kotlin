/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.registerAssembleHierarchicalResourcesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfo
import org.jetbrains.kotlin.gradle.utils.androidExtension

internal suspend fun KotlinAndroidTarget.setUpResourcesPublication(
    compilation: KotlinCompilation<*>,
    variant: String,
) {
    val taskIdentifier = disambiguateName(variant)

    project.multiplatformExtension.resourcesPublicationExtension.subscribeOnPublishResources(this) { resources ->
        project.launch {
            val copyResourcesTask = compilation.registerAssembleHierarchicalResourcesTask(
                taskIdentifier,
                resources,
            )

            // FIXME: Lifecycle issues?
            project.androidExtension.sourceSets.getByName(
                compilation.defaultSourceSet.androidSourceSetInfo.androidSourceSetName
            ).resources.srcDir(copyResourcesTask)
        }
    }

    project.multiplatformExtension.resourcesPublicationExtension.subscribeOnAndroidPublishAssets(this) { resources ->
        // FIXME: Bump AGP to 7.2.2
//        project.launch {
//            project.extensions.findByType<AndroidComponentsExtension<*, *, *>>()?.onVariants {
//                it.sources
//            }
//        }
    }
}