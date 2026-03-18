/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask

internal fun Project.locateOrRegisterSwiftPMDependenciesMetadataTaskAndConsumableConfiguration(
    swiftPMImportExtension: SwiftPMImportExtension,
): TaskProvider<SerializeSwiftPMDependenciesMetadata> {
    val existingTask = project.locateTask<SerializeSwiftPMDependenciesMetadata>(SerializeSwiftPMDependenciesMetadata.TASK_NAME)
    if (existingTask != null) return existingTask

    val swiftPMDependenciesMetadata = project.locateOrRegisterTask<SerializeSwiftPMDependenciesMetadata>(
        SerializeSwiftPMDependenciesMetadata.TASK_NAME,
    ) {
        it.configureWithExtension(swiftPMImportExtension)
    }
    val swiftPMDependenciesMetadataApiElements = registerSwiftPMDependenciesMetadataApiElements(swiftPMDependenciesMetadata)
    project.multiplatformExtension.publishing.adhocSoftwareComponent.addVariantsFromConfiguration(
        swiftPMDependenciesMetadataApiElements
    ) {}
    return swiftPMDependenciesMetadata
}

@DisableCachingByDefault(because = "This task does lightweight serialization that is not worth caching")
internal abstract class SerializeSwiftPMDependenciesMetadata : DefaultTask() {

    @get:Input
    protected abstract val importedSpmModules: SetProperty<SwiftPMDependency>

    @get:Optional
    @get:Input
    protected abstract val iosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    protected abstract val macosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    protected abstract val watchosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    protected abstract val tvosDeploymentVersion: Property<String>

    @get:Input
    protected abstract val discoverModulesImplicitly: Property<Boolean>

    @get:OutputFile
    protected val metadataFile: Provider<RegularFile> = project.layout.buildDirectory.file("kotlin/swiftPMDependenciesMetadata")

    fun configureWithExtension(swiftPMImportExtension: SwiftPMImportExtension) {
        iosDeploymentVersion.set(swiftPMImportExtension.iosMinimumDeploymentTarget)
        macosDeploymentVersion.set(swiftPMImportExtension.macosMinimumDeploymentTarget)
        watchosDeploymentVersion.set(swiftPMImportExtension.watchosMinimumDeploymentTarget)
        tvosDeploymentVersion.set(swiftPMImportExtension.tvosMinimumDeploymentTarget)
        discoverModulesImplicitly.set(swiftPMImportExtension.discoverClangModulesImplicitly)
        importedSpmModules.set(swiftPMImportExtension.swiftPMDependencies)
    }

    @TaskAction
    fun serialize() {
        metadataFile.get().asFile.outputStream().use { file ->
            swiftPMImportMetadata().serializeSwiftPMImportMetadata(file)
        }
    }

    internal fun swiftPMImportMetadata() = SwiftPMImportMetadata(
        iosDeploymentVersion = iosDeploymentVersion.orNull,
        macosDeploymentVersion = macosDeploymentVersion.orNull,
        watchosDeploymentVersion = watchosDeploymentVersion.orNull,
        tvosDeploymentVersion = tvosDeploymentVersion.orNull,
        isModulesDiscoveryEnabled = discoverModulesImplicitly.get(),
        dependencies = importedSpmModules.get(),
    )

    companion object {
        const val TASK_NAME = "serializeSwiftPMDependenciesMetadata"
    }

}