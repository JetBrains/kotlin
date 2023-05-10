/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.project.model.KpmVariant

interface GradleKpmVariant : GradleKpmFragment, KpmVariant {
    val platformType: KotlinPlatformType

    // TODO generalize with KotlinCompilation?
    val compileDependenciesConfiguration: Configuration

    var compileDependencyFiles: FileCollection

    // TODO rewrite using our own artifacts API?
    val compilationOutputs: KotlinCompilationOutput

    // TODO rewrite using our own artifacts API
    val sourceArchiveTaskName: String

    // TODO generalize exposing outputs: what if a variant has more than one such configurations or none?
    val apiElementsConfiguration: Configuration

    val gradleVariantNames: Set<String>
}

interface GradleKpmVariantWithRuntime : GradleKpmVariant {
    // TODO deduplicate with KotlinCompilation?
    val runtimeDependenciesConfiguration: Configuration

    var runtimeDependencyFiles: FileCollection

    val runtimeFiles: ConfigurableFileCollection

    // TODO generalize exposing outputs: what if a variant has more than one such configurations or none?
    val runtimeElementsConfiguration: Configuration
}

interface GradleKpmNativeVariant : GradleKpmVariant {
    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.native

    val hostSpecificMetadataElementsConfiguration: Configuration?

    var enableEndorsedLibraries: Boolean
}

interface PublishedModuleCoordinatesProvider {
    val group: String
    val name: String
    val version: String
    val capabilities: Iterable<String>
}
