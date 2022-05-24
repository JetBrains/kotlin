/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.konan.target.KonanTarget

class GradleKpmNativeVariantConstructor<T : GradleKpmNativeVariantInternal>(
    val konanTarget: KonanTarget,
    val variantClass: Class<T>,
    private val constructor: (
        containingModule: GradleKpmModule,
        fragmentName: String,
        dependencyConfigurations: GradleKpmFragmentDependencyConfigurations,
        compileDependencyConfiguration: Configuration,
        apiElementsConfiguration: Configuration,
        hostSpecificMetadataElementsConfiguration: Configuration?
    ) -> T
) {
    operator fun invoke(
        containingModule: GradleKpmModule,
        fragmentName: String,
        dependencyConfigurations: GradleKpmFragmentDependencyConfigurations,
        compileDependencyConfiguration: Configuration,
        apiElementsConfiguration: Configuration,
        hostSpecificMetadataElementsConfiguration: Configuration?
    ): T = constructor(
        containingModule, fragmentName,
        dependencyConfigurations,
        compileDependencyConfiguration,
        apiElementsConfiguration,
        hostSpecificMetadataElementsConfiguration
    )
}
