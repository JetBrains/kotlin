/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.export.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.registerSwiftExportMetadataApiElements
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.serializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask

/**
 * Registers the [SerializeSwiftExportMetadata] task and the consumable configuration that puts its output into the
 * root component of the publication. Returns the existing task if it was already registered.
 */
internal fun Project.locateOrRegisterSwiftExportMetadataTaskAndConsumableConfiguration(
    swiftExportConfiguration: SwiftExportConfiguration,
): TaskProvider<SerializeSwiftExportMetadata> {
    // The consumable configuration can only be registered once, so guard the whole function rather than the task.
    val existingTask = project.locateTask<SerializeSwiftExportMetadata>(SerializeSwiftExportMetadata.TASK_NAME)
    if (existingTask != null) return existingTask

    val swiftExportMetadata = project.locateOrRegisterTask<SerializeSwiftExportMetadata>(
        SerializeSwiftExportMetadata.TASK_NAME,
    ) {
        it.configureWith(swiftExportConfiguration)
    }
    val swiftExportMetadataApiElements = registerSwiftExportMetadataApiElements(swiftExportMetadata)
    project.multiplatformExtension.publishing.adhocSoftwareComponent.addVariantsFromConfiguration(
        swiftExportMetadataApiElements
    ) {}
    return swiftExportMetadata
}

@DisableCachingByDefault(because = "This task does lightweight serialization that is not worth caching")
internal abstract class SerializeSwiftExportMetadata : DefaultTask() {

    @get:Optional
    @get:Input
    protected abstract val moduleName: Property<String>

    @get:Optional
    @get:Input
    protected abstract val rootPackage: Property<String>

    @get:OutputFile
    protected val metadataFile: Provider<RegularFile> = project.layout.buildDirectory.file("kotlin/swiftExportMetadata")

    fun configureWith(swiftExportConfiguration: SwiftExportConfiguration) {
        moduleName.set(swiftExportConfiguration.moduleName)
        rootPackage.set(swiftExportConfiguration.rootPackage)
    }

    @TaskAction
    fun serialize() {
        metadataFile.get().asFile.outputStream().use { file ->
            swiftExportMetadata().serializeSwiftExportMetadata(file)
        }
    }

    internal fun swiftExportMetadata() = SwiftExportMetadata(
        moduleName = moduleName.orNull,
        rootPackage = rootPackage.orNull,
    )

    companion object {
        const val TASK_NAME = "serializeSwiftExportMetadata"
    }
}
