/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.project.model.KotlinModuleVariant

interface KotlinGradleVariant : KotlinGradleFragment, KotlinModuleVariant {
    val platformType: KotlinPlatformType

    // TODO generalize with KotlinCompilation?
    val compileDependencyConfigurationName: String

    var compileDependencyFiles: FileCollection

    // TODO rewrite using our own artifacts API?
    val compilationOutputs: KotlinCompilationOutput

    // TODO rewrite using our own artifacts API
    val sourceArchiveTaskName: String

    // TODO generalize exposing outputs: what if a variant has more than one such configurations or none?
    val apiElementsConfigurationName: String

    val gradleVariantNames: Set<String>
}

interface KotlinGradleVariantWithRuntime : KotlinGradleVariant {
    // TODO deduplicate with KotlinCompilation?
    val runtimeDependencyConfigurationName: String

    var runtimeDependencyFiles: FileCollection

    val runtimeFiles: ConfigurableFileCollection

    // TODO generalize exposing outputs: what if a variant has more than one such configurations or none?
    val runtimeElementsConfigurationName: String
}

interface KotlinNativeVariant : KotlinGradleVariant {
    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.native

    /** A configuration name for the metadata of the host-specific fragments which this variant includes,
     *  if applies to this Native variant; null otherwise */
    val hostSpecificMetadataElementsConfigurationName: String?

    var enableEndorsedLibraries: Boolean
}

interface SingleMavenPublishedModuleHolder {
    fun assignMavenPublication(publication: MavenPublication)
    val defaultPublishedModuleSuffix: String?
    val publishedMavenModuleCoordinates: PublishedModuleCoordinatesProvider
}

interface PublishedModuleCoordinatesProvider {
    val group: String
    val name: String
    val version: String
    val capabilities: Iterable<String>
}
