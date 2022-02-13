/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.KotlinExternalModelContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragmentInternal

internal fun KotlinGradleFragment.toIdeaKotlinFragment(
    cache: MutableMap<KotlinGradleFragment, IdeaKotlinFragment> = mutableMapOf()
): IdeaKotlinFragment {
    return cache.getOrPut(this) {
        IdeaKotlinFragmentImpl(
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
}
