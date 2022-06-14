/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*

internal fun IdeaKpmProjectBuildingContext.IdeaKpmFragment(fragment: GradleKpmFragment): IdeaKpmFragment {
    return if (fragment is GradleKpmVariant) buildIdeaKpmVariant(fragment)
    else buildIdeaKpmFragment(fragment)
}

private fun IdeaKpmProjectBuildingContext.buildIdeaKpmFragment(fragment: GradleKpmFragment): IdeaKpmFragment {
    return IdeaKpmFragmentImpl(
        coordinates = IdeaKpmFragmentCoordinates(fragment),
        platforms = fragment.containingVariants.map { variant -> IdeaKpmPlatform(variant) }.toSet(),
        languageSettings = IdeaKpmLanguageSettings(fragment.languageSettings),
        dependencies = dependencyResolver.resolve(fragment).toList(),
        contentRoots = fragment.kotlinSourceRoots.sourceDirectories.files.map { file ->
            IdeaKpmContentRootImpl(file, type = IdeaKpmContentRoot.SOURCES_TYPE)
        },
        extras = fragment.extras
    )
}

private fun IdeaKpmProjectBuildingContext.buildIdeaKpmVariant(variant: GradleKpmVariant): IdeaKpmVariant {
    return IdeaKpmVariantImpl(
        fragment = buildIdeaKpmFragment(variant),
        platform = IdeaKpmPlatform(variant),
        variantAttributes = variant.variantAttributes.mapKeys { (key, _) -> key.uniqueName },
        compilationOutputs = IdeaKpmCompilationOutput(variant.compilationOutputs)
    )
}
