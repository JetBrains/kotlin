/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection

abstract class GradleKpmVariantWithRuntimeInternal(
    containingModule: GradleKpmModule,
    fragmentName: String,
    dependencyConfigurations: GradleKpmFragmentDependencyConfigurations,
    compileDependencyConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    final override val runtimeDependenciesConfiguration: Configuration,
    final override val runtimeElementsConfiguration: Configuration
) : GradleKpmVariantInternal(
    containingModule = containingModule,
    fragmentName = fragmentName,
    dependencyConfigurations = dependencyConfigurations,
    compileDependenciesConfiguration = compileDependencyConfiguration,
    apiElementsConfiguration = apiElementsConfiguration
), GradleKpmVariantWithRuntime {
    // TODO deduplicate with KotlinCompilation?

    override var runtimeDependencyFiles: FileCollection = project.files(runtimeDependenciesConfiguration)

    override val runtimeFiles: ConfigurableFileCollection =
        project.files(listOf({ compilationOutputs.allOutputs }, { runtimeDependencyFiles }))
}
