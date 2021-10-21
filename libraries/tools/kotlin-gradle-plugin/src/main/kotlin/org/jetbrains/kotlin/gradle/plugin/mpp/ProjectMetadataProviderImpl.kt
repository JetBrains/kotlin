/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.targets.native.internal.*
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier

internal fun ProjectMetadataProvider(
    dependencyProject: Project,
    moduleIdentifier: KotlinModuleIdentifier
): ProjectMetadataProvider {
    return ProjectMetadataProviderImpl(dependencyProject, moduleIdentifier)
}

private class ProjectMetadataProviderImpl(
    private val dependencyProject: Project,
    private val moduleIdentifier: KotlinModuleIdentifier
) : ProjectMetadataProvider() {
    override fun getSourceSetCompiledMetadata(sourceSetName: String): FileCollection {
        return when (val projectExtension = dependencyProject.topLevelExtension) {
            is KotlinMultiplatformExtension -> projectExtension.targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME).compilations
                .firstOrNull { it.name == sourceSetName }
                ?.output?.classesDirs ?: dependencyProject.files()

            is KotlinPm20ProjectExtension -> {
                val moduleId = moduleIdentifier
                val module = projectExtension.modules.single { it.moduleIdentifier == moduleId }
                val metadataCompilationRegistry = projectExtension.metadataCompilationRegistryByModuleId.getValue(moduleId)
                metadataCompilationRegistry.byFragment(module.fragments.getByName(sourceSetName)).output.classesDirs
            }
            else -> error("unexpected top-level Kotlin extension $projectExtension")
        }
    }

    override fun getSourceSetCInteropMetadata(sourceSetName: String, consumer: MetadataConsumer): FileCollection {
        val multiplatformExtension = dependencyProject.topLevelExtension as? KotlinMultiplatformExtension
            ?: return dependencyProject.files()

        val commonizeCInteropTask = when (consumer) {
            MetadataConsumer.Ide -> dependencyProject.copyCommonizeCInteropForIdeTask ?: return dependencyProject.files()
            MetadataConsumer.Cli -> dependencyProject.commonizeCInteropTask ?: return dependencyProject.files()
        }

        val sourceSet = multiplatformExtension.sourceSets.findByName(sourceSetName) ?: return dependencyProject.files()
        val dependent = CInteropCommonizerDependent.from(dependencyProject, sourceSet) ?: return dependencyProject.files()
        return commonizeCInteropTask.get().commonizedOutputLibraries(dependent)
    }
}
