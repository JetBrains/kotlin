/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.ConfigurationPublications
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSoftwareComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext

private fun ConfigurationPublications.capabilityFromCoorinates(coordinates: ModuleVersionIdentifier) {
    capability("${coordinates.group}:${coordinates.name}:${coordinates.version}")
}

private fun ModuleVersionIdentifier.isValidForCapability(): Boolean =
    !group.isNullOrEmpty() && !name.isNullOrEmpty() && !version.isNullOrEmpty()

/**
 * Creates DefaultKotlinUsage, with given arguments.
 *
 * When such a usage would be published using [org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetSoftwareComponent],
 * the `-published` version of configuration would be created, and used as one actually published.
 *
 * This configuration would always extend [dependencyConfigurationName], and also use its attributes.
 *
 * If [replacementTaskProvider] has no value, artifacts from [dependencyConfigurationName] are used.
 * If [replacementTaskProvider] has a value:
 *   - Artifacts from original configuration are ignored,
 *   - Task outputs are used as artifacts
 *   - Additional configuration is performed by [extraReplacementArtifactConfiguration] and [extraReplacementAttributesProvider].
 *   - Additional capabilities are set if [movedToSoftwareComponent] is not null and don't match with software component usage was initially for
 */
internal fun defaultKotlinUsageContextWithArtifactsMaybeReplacedByTask(
    replacementTaskProvider: Provider<TaskProvider<*>>,
    extraReplacementArtifactConfiguration: ConfigurablePublishArtifact.() -> Unit = {},
    extraReplacementAttributesProvider: AttributeContainer.() -> Unit = {},
    movedToSoftwareComponent: KotlinSoftwareComponent?,
    compilation: KotlinCompilation<*>,
    mavenScope: KotlinUsageContext.MavenScope?,
    dependencyConfigurationName: String,
    overrideConfigurationAttributes: AttributeContainer? = null,
    includeIntoProjectStructureMetadata: Boolean = true,
    publishOnlyIf: DefaultKotlinUsageContext.PublishOnlyIf = DefaultKotlinUsageContext.PublishOnlyIf { true },
): DefaultKotlinUsageContext {
    val overrideConfigurationArtifacts: Provider<Set<PublishArtifact>>? = replacementTaskProvider.map { emptySet() }
    return DefaultKotlinUsageContext(
        compilation = compilation,
        mavenScope = mavenScope,
        dependencyConfigurationName = dependencyConfigurationName,
        includeIntoProjectStructureMetadata = includeIntoProjectStructureMetadata,
        publishOnlyIf = publishOnlyIf,
        overrideConfigurationAttributes = overrideConfigurationAttributes,
        overrideConfigurationArtifacts = overrideConfigurationArtifacts,
        configurePublishedConfiguration = conf@{ kotlinComponent ->
            val replacementTask = replacementTaskProvider.orNull ?: return@conf
            outgoing.artifact(replacementTask) {
                it.extraReplacementArtifactConfiguration()
            }
            attributes.extraReplacementAttributesProvider()
            if (kotlinComponent is ComponentWithCoordinates && movedToSoftwareComponent is ComponentWithCoordinates) {
                // It can be invalid, if no publications are configured. We don't care about capabilities in that case
                if (kotlinComponent.coordinates.isValidForCapability() && movedToSoftwareComponent.coordinates.isValidForCapability()) {
                    outgoing.capabilityFromCoorinates(kotlinComponent.coordinates)
                    outgoing.capabilityFromCoorinates(movedToSoftwareComponent.coordinates)
                }
            }
        }
    )
}


internal fun Project.defaultKotlinUsageContextMaybeReplacedWithKar(
    isStoredInKotlinArchive: Provider<Boolean>,
    compilation: KotlinCompilation<*>,
    mavenScope: KotlinUsageContext.MavenScope?,
    dependencyConfigurationName: String,
    includeIntoProjectStructureMetadata: Boolean = true,
    publishOnlyIf: DefaultKotlinUsageContext.PublishOnlyIf = DefaultKotlinUsageContext.PublishOnlyIf { true },
): DefaultKotlinUsageContext {
    return defaultKotlinUsageContextWithArtifactsMaybeReplacedByTask(
        replacementTaskProvider = isStoredInKotlinArchive.map { if (it) karPackTask else null },
        extraReplacementArtifactConfiguration = {
            extension = KarLayout.KAR_XZ_PACKED_EXTENSION
        },
        extraReplacementAttributesProvider = {
            attribute(KarLayout.Attributes.compressionMethod, KarLayout.Attributes.CompressionMethod.XZ)
        },
        movedToSoftwareComponent = multiplatformExtensionOrNull?.rootSoftwareComponent,
        compilation = compilation,
        mavenScope = mavenScope,
        dependencyConfigurationName = dependencyConfigurationName,
        includeIntoProjectStructureMetadata = includeIntoProjectStructureMetadata,
        publishOnlyIf = publishOnlyIf,
    )
}
