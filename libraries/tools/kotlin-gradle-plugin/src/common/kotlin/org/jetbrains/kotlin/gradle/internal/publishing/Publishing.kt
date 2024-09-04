/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.JsonUtils

@DisableCachingByDefault(because = "Only I/O operations")
internal abstract class ExportKotlinPublishCoordinatesTask : DefaultTask() {

    @get:OutputFile
    abstract val outputJsonFile: RegularFileProperty

    @get:Nested
    abstract val data: Property<PublicationCoordinatesProperty>

    @TaskAction
    fun action() {

        val file = outputJsonFile.get().asFile
        val json = JsonUtils.gson.toJson(data.get().toPublicationCoordinates())
        file.writeText(json)
    }
}

internal data class PublicationCoordinatesProperty(
    @get:Input
    val rootGroup: Provider<String>,

    @get:Input
    val rootArtifactId: Provider<String>,

    @get:Input
    val rootVersion: Provider<String>,

    @get:Input
    val targetGroup: Provider<String>,

    @get:Input
    val targetArtifactId: Provider<String>,

    @get:Input
    val targetVersion: Provider<String>,
) {
    fun toPublicationCoordinates(): PublicationCoordinates {
        return PublicationCoordinates(
            rootPublicationGAV = GAV(rootGroup.get(), rootArtifactId.get(), rootVersion.get()),
            targetPublicationGAV = GAV(targetGroup.get(), targetArtifactId.get(), targetVersion.get())
        )
    }
}

internal data class PublicationCoordinates(val rootPublicationGAV: GAV, val targetPublicationGAV: GAV)
internal data class GAV(val group: String, val artifactId: String, val version: String)
