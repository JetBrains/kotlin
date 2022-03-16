/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.KotlinExternalModelContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*

internal fun IdeaKotlinProjectModelBuildingContext.IdeaKotlinFragment(fragment: KotlinGradleFragment): IdeaKotlinFragment {
    return if (fragment is KotlinGradleVariant) buildIdeaKotlinVariant(fragment)
    else buildIdeaKotlinFragment(fragment)
}

private fun IdeaKotlinProjectModelBuildingContext.buildIdeaKotlinFragment(fragment: KotlinGradleFragment): IdeaKotlinFragment {
    return IdeaKotlinFragmentImpl(
        coordinates = IdeaKotlinFragmentCoordinates(fragment),
        platforms = fragment.containingVariants.map { variant -> buildIdeaKotlinPlatform(variant) }.toSet(),
        languageSettings = fragment.languageSettings.toIdeaKotlinLanguageSettings(),
        dependencies = dependencyResolver.resolve(fragment).toList(),
        sourceDirectories = fragment.kotlinSourceRoots.sourceDirectories.files.toList().map { file -> IdeaKotlinSourceDirectoryImpl(file) },
        resourceDirectories = emptyList(),
        external = (fragment as? KotlinGradleFragmentInternal)?.external ?: KotlinExternalModelContainer.Empty
    )
}

private fun IdeaKotlinProjectModelBuildingContext.buildIdeaKotlinVariant(variant: KotlinGradleVariant): IdeaKotlinVariant {
    return IdeaKotlinVariantImpl(
        fragment = buildIdeaKotlinFragment(variant),
        platform = buildIdeaKotlinPlatform(variant),
        variantAttributes = variant.variantAttributes.mapKeys { (key, _) -> key.uniqueName },
        compilationOutputs = variant.compilationOutputs.toIdeaKotlinCompilationOutputs()
    )
}
