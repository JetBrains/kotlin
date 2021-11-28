/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.mpp.MavenPublicationCoordinatesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.project.model.KotlinAttributeKey
import org.jetbrains.kotlin.project.model.KotlinPlatformTypeAttribute

abstract class KotlinGradleVariantInternal(
    containingModule: KotlinGradleModule,
    override val fragmentName: String
) : KotlinGradleFragmentInternal(containingModule, fragmentName), KotlinGradleVariant {

    override val variantAttributes: Map<KotlinAttributeKey, String>
        get() = mapOf(KotlinPlatformTypeAttribute to kotlinPlatformTypeAttributeFromPlatform(platformType)) // TODO user attributes

    // TODO generalize with KotlinCompilation?
    override val compileDependencyConfigurationName: String
        get() = disambiguateName("CompileDependencies")

    override lateinit var compileDependencyFiles: FileCollection

    internal abstract val compilationData: KotlinVariantCompilationDataInternal<*>

    // TODO rewrite using our own artifacts API?
    override val compilationOutputs: KotlinCompilationOutput =
        DefaultKotlinCompilationOutput(
            project,
            project.provider { project.buildDir.resolve("processedResources/${containingModule.name}/${fragmentName}") }
        )

    // TODO rewrite using our own artifacts API
    override val sourceArchiveTaskName: String
        get() = defaultSourceArtifactTaskName

    // TODO generalize exposing outputs: what if a variant has more than one such configurations or none?
    override val apiElementsConfigurationName: String
        get() = disambiguateName("apiElements")

    override fun toString(): String = "variant $fragmentName in $containingModule"
}

internal val KotlinGradleVariant.compileDependencyConfiguration: Configuration
    get() = containingModule.project.configurations.getByName(compileDependencyConfigurationName)

class DefaultSingleMavenPublishedModuleHolder(
    private var module: KotlinGradleModule,
    override val defaultPublishedModuleSuffix: String?
) : SingleMavenPublishedModuleHolder {
    private val project get() = module.project

    private var assignedMavenPublication: MavenPublication? = null

    override fun assignMavenPublication(publication: MavenPublication) {
        if (assignedMavenPublication != null)
            error("already assigned publication $publication")
        assignedMavenPublication = publication
    }

    override val publishedMavenModuleCoordinates: PublishedModuleCoordinatesProvider =
        MavenPublicationCoordinatesProvider(
            project,
            { assignedMavenPublication },
            defaultPublishedModuleSuffix,
            capabilities = listOfNotNull(ComputedCapability.capabilityStringFromModule(module))
        )
}

private fun kotlinPlatformTypeAttributeFromPlatform(platformType: KotlinPlatformType) = platformType.name

// TODO: rewrite with the artifacts API
internal val KotlinGradleVariant.defaultSourceArtifactTaskName: String
    get() = disambiguateName("sourcesJar")

abstract class KotlinGradleVariantWithRuntimeInternal(
    containingModule: KotlinGradleModule,
    fragmentName: String
) : KotlinGradleVariantInternal(containingModule, fragmentName), KotlinGradleVariantWithRuntime {
    // TODO deduplicate with KotlinCompilation?
    override val runtimeDependencyConfigurationName: String
        get() = disambiguateName("RuntimeDependencies")

    override lateinit var runtimeDependencyFiles: FileCollection

    override val runtimeFiles: ConfigurableFileCollection = project.files(
        listOf(
            { compilationOutputs.allOutputs },
            { runtimeDependencyFiles }
        )
    )

    // TODO generalize exposing outputs: what if a variant has more than one such configurations or none?
    override val runtimeElementsConfigurationName: String
        get() = disambiguateName("runtimeElements")
}

private fun defaultModuleSuffix(module: KotlinGradleModule, variantName: String): String =
    dashSeparatedName(variantName, module.moduleClassifier)

abstract class KotlinGradlePublishedVariantWithRuntime(containingModule: KotlinGradleModule, fragmentName: String) :
    KotlinGradleVariantWithRuntimeInternal(containingModule, fragmentName),
    SingleMavenPublishedModuleHolder by DefaultSingleMavenPublishedModuleHolder(
        containingModule,
        defaultModuleSuffix(containingModule, fragmentName)
    ) {

    override val gradleVariantNames: Set<String>
        get() =
            listOf(apiElementsConfigurationName, runtimeElementsConfigurationName).flatMapTo(mutableSetOf()) {
                listOf(it, publishedConfigurationName(it))
            }
}