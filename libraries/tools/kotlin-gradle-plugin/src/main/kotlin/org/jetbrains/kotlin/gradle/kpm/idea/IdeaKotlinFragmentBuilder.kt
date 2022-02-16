/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.KotlinExternalModelContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragmentInternal
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant

private typealias FragmentInterner = MutableMap<KotlinGradleFragment, IdeaKotlinFragment>

internal fun KotlinGradleFragment.toIdeaKotlinFragment(
    cache: FragmentInterner = mutableMapOf()
): IdeaKotlinFragment {
    return cache.getOrPut(this) {
        if (this is KotlinGradleVariant) buildIdeaKotlinVariant(cache)
        else buildIdeaKotlinFragment(cache)
    }
}

internal fun KotlinGradleVariant.toIdeaKotlinVariant(
    cache: FragmentInterner = mutableMapOf()
): IdeaKotlinVariant = toIdeaKotlinVariant(cache) as IdeaKotlinVariant

private fun KotlinGradleFragment.buildIdeaKotlinFragment(cache: FragmentInterner): IdeaKotlinFragment {
    return IdeaKotlinFragmentImpl(
        name = name,
        languageSettings = languageSettings.toIdeaKotlinLanguageSettings(),
        dependencies = emptyList(),
        directRefinesDependencies = directRefinesDependencies.map { refinesFragment ->
            refinesFragment.toIdeaKotlinFragment(cache)
        },
        sourceDirectories = kotlinSourceRoots.sourceDirectories.files.toList().map { file ->
            IdeaKotlinSourceDirectoryImpl(file)
        },
        resourceDirectories = emptyList(),
        external = (this as? KotlinGradleFragmentInternal)?.external ?: KotlinExternalModelContainer.Empty
    )
}

private fun KotlinGradleVariant.buildIdeaKotlinVariant(cache: FragmentInterner): IdeaKotlinVariant {
    return IdeaKotlinVariantImpl(
        fragment = buildIdeaKotlinFragment(cache),
        variantAttributes = variantAttributes.mapKeys { (key, _) -> key.uniqueName },
        compilationOutputs = compilationOutputs.toIdeaKotlinCompilationOutputs()
    )
}
