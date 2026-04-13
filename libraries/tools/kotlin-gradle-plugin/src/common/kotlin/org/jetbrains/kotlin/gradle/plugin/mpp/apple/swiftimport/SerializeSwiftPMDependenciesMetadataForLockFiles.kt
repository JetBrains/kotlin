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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.ObjectOutputStream
import java.io.Serializable

internal fun Project.locateOrRegisterSwiftPMDependenciesMetadataTaskForLockFilesAndConsumableConfiguration(
    swiftPMImportExtension: SwiftPMImportExtension,
    transitiveDependencies: Provider<TransitiveSwiftPMDependencies>,
    konanTarget: KonanTarget,
): TaskProvider<SerializeSwiftPMDependenciesMetadataForLockFiles> {
    val existingTask =
        project.locateTask<SerializeSwiftPMDependenciesMetadataForLockFiles>(SerializeSwiftPMDependenciesMetadataForLockFiles.TASK_NAME)
    if (existingTask != null) return existingTask
    val swiftPMDependenciesMetadata = project.locateOrRegisterTask<SerializeSwiftPMDependenciesMetadataForLockFiles>(
        SerializeSwiftPMDependenciesMetadataForLockFiles.TASK_NAME,
    ) {
        it.enabled = HostManager.hostIsMac
        it.configureWithExtension(swiftPMImportExtension)
        it.transitiveSwiftPMDependencies.set(transitiveDependencies)
        it.konanTargets.add(konanTarget)
    }
    registerSwiftPMDependenciesMetadataForLockFilesApiElements(swiftPMDependenciesMetadata)
    return swiftPMDependenciesMetadata
}

internal data class SwiftPMImportMetadataForLockFiles(
    val konanTargets : Set<KonanTarget>,
    val projectPath: String,
    val iosDeploymentVersion: String?,
    val macosDeploymentVersion: String?,
    val watchosDeploymentVersion: String?,
    val tvosDeploymentVersion: String?,
    val directDependencies: Set<SwiftPMDependency>,
    val transitiveDependencies: TransitiveSwiftPMDependencies,
) : Serializable

@DisableCachingByDefault(because = "This task does lightweight serialization that is not worth caching")
internal abstract class SerializeSwiftPMDependenciesMetadataForLockFiles : DefaultTask() {

    @get:Input
    abstract val konanTargets: SetProperty<KonanTarget>

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
    internal val metadataFile: Provider<RegularFile> = project.layout.buildDirectory.file("kotlin/swiftPMDependenciesMetadataForLockFiles")

    @get:Internal
    abstract val transitiveSwiftPMDependencies: Property<TransitiveSwiftPMDependencies>

    @get:Input
    protected val fingerprint = if (GradleVersion.current().baseVersion >= GradleVersion.version("8.0")) {
        transitiveSwiftPMDependencies
    } else {
        project.provider { 0 }
    }

    private val projectPath = project.path

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
            ObjectOutputStream(file).use {
                it.writeObject(
                    SwiftPMImportMetadataForLockFiles(
                        konanTargets = konanTargets.get().toSet(),
                        iosDeploymentVersion = iosDeploymentVersion.orNull,
                        macosDeploymentVersion = macosDeploymentVersion.orNull,
                        watchosDeploymentVersion = watchosDeploymentVersion.orNull,
                        tvosDeploymentVersion = tvosDeploymentVersion.orNull,
                        directDependencies = importedSpmModules.get().toSet(),
                        transitiveDependencies = transitiveSwiftPMDependencies.get(),
                        projectPath = projectPath,
                    )
                )
            }
        }
    }

    companion object {
        const val TASK_NAME = "serializeSwiftPMDependenciesMetadataForLockFiles"
    }

}
