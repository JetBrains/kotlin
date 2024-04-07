/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.assembleHierarchicalResources
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget


internal val SetUpMultiplatformJvmResourcesPublicationAction = KotlinProjectSetupAction {
    if (!kotlinPropertiesProvider.mppResourcesPublication) return@KotlinProjectSetupAction

    multiplatformExtension.targets.all { target ->
        if (target is KotlinJvmTarget) {
            target.setUpMultiplatformResourcesAndAssets()
        }
    }
}

private fun KotlinJvmTarget.setUpMultiplatformResourcesAndAssets() {
    project.multiplatformExtension.resourcesPublicationExtension?.subscribeOnPublishResources(
        this
    ) { resources ->
        val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        val copyResourcesTask = mainCompilation.assembleHierarchicalResources(
            targetName,
            resources,
        )
        mainCompilation.defaultSourceSet.resources.srcDir(copyResourcesTask)
    }
}