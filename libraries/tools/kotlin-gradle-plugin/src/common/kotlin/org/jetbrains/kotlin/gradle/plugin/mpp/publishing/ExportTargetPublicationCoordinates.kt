/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinShareableDataAsSecondaryVariant
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinProjectSharedDataProvider
import org.jetbrains.kotlin.gradle.plugin.internal.kotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.originalVariantNameFromPublished

internal class TargetPublicationCoordinates(
    @get:Nested
    val rootPublicationCoordinates: GAV,
    @get:Nested
    val targetPublicationCoordinates: GAV,
    @get:Input
    val mavenScope: MavenScope,
): KotlinShareableDataAsSecondaryVariant {
    internal class GAV(
        @get:Input
        val group: String,
        @get:Input
        val artifactId: String,
        @get:Input
        val version: String
    )
}

internal val ComponentWithCoordinates.gavCoordinates: TargetPublicationCoordinates.GAV
    get(): TargetPublicationCoordinates.GAV {
        val coordinates = coordinates
        return TargetPublicationCoordinates.GAV(
            group = coordinates.group,
            artifactId = coordinates.name,
            version = coordinates.version
        )
    }


internal val ExportTargetPublicationCoordinates = KotlinProjectSetupCoroutine {
    val multiplatform = multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine
    multiplatform.awaitTargets().all { target ->
        if (target is KotlinMetadataTarget) return@all // not needed for POM Dependencies Rewriter
        project.launch { project.exportForPomDependenciesRewriter(target) }
    }
}

private const val PROJECT_DATA_SHARING_KEY = "targetPublicationCoordinates"

private suspend fun Project.exportForPomDependenciesRewriter(target: KotlinTarget) {
    // Non-default publication layouts are not supported for pom rewriting
    if (!project.kotlinPropertiesProvider.createDefaultMultiplatformPublications) return

    project.kotlinPluginLifecycle.await(KotlinPluginLifecycle.Stage.AfterFinaliseDsl)

    val rootComponent = multiplatformExtension.rootSoftwareComponent
    if (rootComponent !is ComponentWithCoordinates) return

    val projectDataSharingService = project.kotlinSecondaryVariantsDataSharing
    for (targetComponent in target.internal.kotlinComponents) {
        if (targetComponent !is ComponentWithCoordinates) continue
        for (usage in targetComponent.internal.usages) {
            // FIXME: Why?
            val mavenScope = usage.mavenScope ?: continue

            val configurationName = originalVariantNameFromPublished(usage.dependencyConfigurationName) ?: usage.dependencyConfigurationName
            val configuration = configurations.getByName(configurationName)
            projectDataSharingService.shareDataFromProvider(PROJECT_DATA_SHARING_KEY, configuration, provider {
                TargetPublicationCoordinates(
                    rootPublicationCoordinates = rootComponent.gavCoordinates,
                    targetPublicationCoordinates = targetComponent.gavCoordinates,
                    mavenScope = mavenScope,
                )
            })
        }
    }
}

internal fun KotlinSecondaryVariantsDataSharing.consumeTargetPublicationCoordinates(
    from: Configuration
): KotlinProjectSharedDataProvider<TargetPublicationCoordinates> = consume(
    key = PROJECT_DATA_SHARING_KEY,
    incomingConfiguration = from,
    clazz = TargetPublicationCoordinates::class.java
)