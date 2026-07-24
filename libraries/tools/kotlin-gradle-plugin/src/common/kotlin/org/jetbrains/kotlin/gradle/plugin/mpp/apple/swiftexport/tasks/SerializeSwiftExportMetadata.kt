/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.registerSwiftExportMetadataApiElements
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.serializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.tasks.registerTask

/**
 * Registers the [SerializeSwiftExportMetadata] task and the consumable configuration that puts its output into the
 * root component of the publication.
 */
internal fun Project.registerSwiftExportMetadataTaskAndConsumableConfiguration(
    swiftExportExtension: SwiftExportExtension,
): TaskProvider<SerializeSwiftExportMetadata> {
    val swiftExportMetadata = project.registerTask<SerializeSwiftExportMetadata>(
        SerializeSwiftExportMetadata.TASK_NAME,
    ) {
        it.configureWithExtension(swiftExportExtension)
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
    protected abstract val flattenPackage: Property<String>

    @get:OutputFile
    protected val metadataFile: Provider<RegularFile> = project.layout.buildDirectory.file("kotlin/swiftExportMetadata")

    fun configureWithExtension(swiftExportExtension: SwiftExportExtension) {
        moduleName.set(swiftExportExtension.moduleName)
        flattenPackage.set(swiftExportExtension.flattenPackage)
    }

    @TaskAction
    fun serialize() {
        metadataFile.get().asFile.outputStream().use { file ->
            swiftExportMetadata().serializeSwiftExportMetadata(file)
        }
    }

    internal fun swiftExportMetadata() = SwiftExportMetadata(
        moduleName = moduleName.orNull,
        flattenPackage = flattenPackage.orNull,
    )

    companion object {
        const val TASK_NAME = "serializeSwiftExportMetadata"
    }
}
