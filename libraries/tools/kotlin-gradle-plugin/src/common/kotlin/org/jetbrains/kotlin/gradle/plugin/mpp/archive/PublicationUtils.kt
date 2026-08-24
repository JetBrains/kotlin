/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationPublications
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

private fun Configuration.addOutgoingKarArtifactTo(karPackTask: TaskProvider<PackKotlinArchiveTask>) {
    outgoing.artifact(karPackTask) {
        it.extension = KarLayout.KAR_XZ_PACKED_EXTENSION
    }
    outgoing.attributes.attribute(KarLayout.Attributes.compressionMethod, KarLayout.Attributes.CompressionMethod.XZ)
}

private fun ConfigurationPublications.capabilityFromCoorinates(coordinates: ModuleVersionIdentifier) {
    capability("${coordinates.group}:${coordinates.name}:${coordinates.version}")
}

private fun ModuleVersionIdentifier.isValidForCapability(): Boolean =
    !group.isNullOrEmpty() && !name.isNullOrEmpty() && !version.isNullOrEmpty()

internal fun Project.defaultKotlinUsageContextMaybeReplacedWithKar(
    isStoredInKotlinArchive: Provider<Boolean>?,
    compilation: KotlinCompilation<*>,
    mavenScope: KotlinUsageContext.MavenScope?,
    dependencyConfigurationName: String,
    includeIntoProjectStructureMetadata: Boolean = true,
    publishOnlyIf: DefaultKotlinUsageContext.PublishOnlyIf = DefaultKotlinUsageContext.PublishOnlyIf { true },
): DefaultKotlinUsageContext {
    val overrideConfigurationArtifacts: Provider<Set<PublishArtifact>>? = isStoredInKotlinArchive?.map { if (it) emptySet<PublishArtifact>() else null }
    val karTaskProvider = karPackTask
    val rootSoftwareComponent = project.multiplatformExtensionOrNull?.rootSoftwareComponent
    return DefaultKotlinUsageContext(
        compilation = compilation,
        mavenScope = mavenScope,
        dependencyConfigurationName = dependencyConfigurationName,
        includeIntoProjectStructureMetadata = includeIntoProjectStructureMetadata,
        publishOnlyIf = publishOnlyIf,
        overrideConfigurationArtifacts = overrideConfigurationArtifacts,
        configurePublishedConfiguration = { kotlinComponent ->
            if (isStoredInKotlinArchive?.orNull == true) {
                addOutgoingKarArtifactTo(karTaskProvider)
                if (kotlinComponent is ComponentWithCoordinates && rootSoftwareComponent is ComponentWithCoordinates) {
                    // It can be invalid, if no publications are configured. We don't care about capabilities in that case
                    if (kotlinComponent.coordinates.isValidForCapability() && rootSoftwareComponent.coordinates.isValidForCapability()) {
                        outgoing.capabilityFromCoorinates(kotlinComponent.coordinates)
                        outgoing.capabilityFromCoorinates(rootSoftwareComponent.coordinates)
                    }
                }
            }
        }
    )
}
