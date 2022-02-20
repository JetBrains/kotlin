/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule

internal fun KotlinGradleModule.toIdeaKotlinModule(): IdeaKotlinModule {
    val fragmentsCache = mutableMapOf<KotlinGradleFragment, IdeaKotlinFragment>()
    return IdeaKotlinModuleImpl(
        moduleIdentifier = moduleIdentifier.toIdeaKotlinModuleIdentifier(),
        fragments = fragments.toList().map { fragment -> fragment.toIdeaKotlinFragment(fragmentsCache) }
    )
}
