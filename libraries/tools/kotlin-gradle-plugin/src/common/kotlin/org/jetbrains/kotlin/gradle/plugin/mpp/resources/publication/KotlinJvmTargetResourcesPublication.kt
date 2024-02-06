/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.registerAssembleHierarchicalResourcesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget


internal val SetUpMultiplatformJvmResourcesPublicationAction = KotlinProjectSetupAction {
    project.launch {
        project.multiplatformExtension.awaitTargets()
            .filterIsInstance<KotlinJvmTarget>()
            .forEach { target ->
                target.setUpResourcesPublication()
            }
    }
}

private suspend fun KotlinJvmTarget.setUpResourcesPublication() {
    project.multiplatformExtension.resourcesPublicationExtension.subscribeOnPublishResources(
        this
    ) { resources ->
        val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        project.launch {
            val copyResourcesTask = mainCompilation.registerAssembleHierarchicalResourcesTask(
                targetName,
                resources,
            )
            mainCompilation.defaultSourceSet.resources.srcDir(copyResourcesTask)
        }
    }
}