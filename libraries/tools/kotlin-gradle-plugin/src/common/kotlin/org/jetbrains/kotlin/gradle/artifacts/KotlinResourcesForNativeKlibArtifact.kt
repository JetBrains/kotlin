/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.registerTask

internal val KotlinResourcesForNativeKlibArtifact = KotlinTargetArtifact { target, apiElements, _ ->
    if (target !is KotlinNativeTarget) return@KotlinTargetArtifact
    val project = target.project
    project.kotlinPluginLifecycle.await(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations)

    val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) ?: return@KotlinTargetArtifact
    val taskIdentifier = target.name.capitalize()
    val archiveName = "kotlin_resources.zip"

    val zipResourcesTask = project.registerTask<Zip>("bundleResources${taskIdentifier}") { copy ->
        copy.archiveFileName.set(archiveName)
        copy.destinationDirectory.set(project.layout.buildDirectory.dir("resourcesFor${taskIdentifier}"))
        copy.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        mainCompilation.copyResourcesByHashing(copy)
    }

    // 2. Output the zip in the api elements configuration
    val apiElementsName = mainCompilation.target.apiElementsConfigurationName
    val kotlinResourceArtifact = mainCompilation.project.artifacts.add(
        apiElementsName,
        zipResourcesTask.flatMap { it.archiveFile }
    ) { artifact ->
        artifact.name = mainCompilation.compilationName
        artifact.extension = archiveName
        artifact.type = archiveName
        artifact.classifier = null
        artifact.builtBy(zipResourcesTask)
    }
    mainCompilation.project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(kotlinResourceArtifact)
    project.configurations.getByName(apiElementsName).artifacts.add(kotlinResourceArtifact)
}
