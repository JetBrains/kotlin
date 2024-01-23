/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceAnd


/**
 * 1. Copy all composeResourceDirectories
 * 2. Zip it all up
 * 3. Output it the resources configuration which is published as a SoftwareComponentVariant
 * */
internal val KotlinResourcesForNativeKlibAction = KotlinProjectSetupAction {
    val project = this
    project.launch {
        project.kotlinPluginLifecycle.await(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations)
        project.multiplatformExtension.targets
            .filterIsInstance<AbstractKotlinTarget>()
            .filter { it is KotlinNativeTarget || it is KotlinJsIrTarget }
            .forEach {
                val target = it

                // FIXME: There is now an empty publication
                if (target.composeResourceDirectories.isEmpty()) return@forEach

                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) ?: return@forEach
                val taskIdentifier = target.name.capitalize()
                val archiveName = "kotlin_resources.zip"
                val resourcesBaseCopyDirectory = project.layout.buildDirectory.dir("resourcesForNative${taskIdentifier}")

                // FIXME: Check that zip resources task actually executes
                val zipResourcesTask = project.registerTask<Zip>("bundleResources${taskIdentifier}") { copy ->
                    // FIXME: Specify name properly???
//                    copy.archiveFileName.set(dashSeparatedName(target.targetName.toLowerCaseAsciiOnly(), archiveName))
                    copy.destinationDirectory.set(project.layout.buildDirectory.dir("zippedResourcesFor${taskIdentifier}"))
                    copy.duplicatesStrategy = DuplicatesStrategy.FAIL
                    copy.from(resourcesBaseCopyDirectory)
                }

                mainCompilation.registerCopyResourcesTasks(
                    "ResourcesForNativeCompilation${taskIdentifier}",
                    target.composeResourceDirectories,
                    resourcesBaseCopyDirectory,
                ).forEach { copyTask ->
                    zipResourcesTask.dependsOn(copyTask)
                }

                // 2. Output the zip in the resources configuration
                val resourcesConfigurationName = mainCompilation.target.resourcesElementsConfigurationName
                // FIXME: Support secondary variant without zipping?
                mainCompilation.project.artifacts.add(
                    resourcesConfigurationName,
                    zipResourcesTask
                ) { artifact ->
                    artifact.extension = archiveName
                    artifact.type = archiveName
                    artifact.classifier = null
                    artifact.builtBy(zipResourcesTask)
                }
            }
    }
}
