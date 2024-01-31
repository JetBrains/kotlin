/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask


internal val SetUpKotlinResourcesForKlibBasedPublicationAction = KotlinProjectSetupAction {
    launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
        multiplatformExtension.targets
            .filterIsInstance<AbstractKotlinTarget>()
            .filter { isTargetPublicationKlibBased(it) }
            .forEach { setUpResourcesConfiguration(it) }
    }
}

private fun isTargetPublicationKlibBased(target: KotlinTarget) = target is KotlinNativeTarget || target is KotlinJsIrTarget

/**
 * 1. Copy all composeResourceDirectories into resourcesBaseCopyDirectory
 * 2. Zip up resourcesBaseCopyDirectory
 * 3. Output the zip in the resources configuration which is published as a SoftwareComponentVariant
 * */
private fun setUpResourcesConfiguration(target: AbstractKotlinTarget) {
    // FIXME: There is now an empty publication
    if (target.composeResourceDirectories.isEmpty()) return

    val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) ?: return
    val project = target.project
    val taskIdentifier = target.disambiguationClassifier ?: target.targetName
    val archiveName = "kotlin_resources.zip"
    val resourcesBaseCopyDirectory = project.layout.buildDirectory.dir("${taskIdentifier}ResourcesForKlibBasedTarget")
    val zippedResourcesDirectory = project.layout.buildDirectory.dir("${taskIdentifier}ZippedResources")

    // FIXME: Check that zip resources task actually executes. What happens if all composeResourceDirectories are empty/don't exit?
    val zipResourcesTask = project.registerTask<Zip>("${taskIdentifier}ZipResources") { copy ->
        copy.destinationDirectory.set(zippedResourcesDirectory)
        copy.duplicatesStrategy = DuplicatesStrategy.FAIL
        copy.from(resourcesBaseCopyDirectory)
    }

    mainCompilation.registerCopyComposeResourcesTasks(
        "${taskIdentifier}ResourcesForKlibBasedCompilation",
        target.composeResourceDirectories,
        resourcesBaseCopyDirectory,
    ).forEach { copyTask ->
        zipResourcesTask.dependsOn(copyTask)
    }

    val resourcesConfigurationName = mainCompilation.target.resourcesElementsConfigurationName
    // FIXME: Support secondary variant without zipping?
    project.artifacts.add(
        resourcesConfigurationName,
        zipResourcesTask
    ) { artifact ->
        artifact.extension = archiveName
        artifact.type = archiveName
        artifact.classifier = null
        artifact.builtBy(zipResourcesTask)
    }
}