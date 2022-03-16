/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule

internal fun IdeaKotlinProjectModelBuildingContext.IdeaKotlinModule(module: KotlinGradleModule): IdeaKotlinModule {
    return IdeaKotlinModuleImpl(
        coordinates = IdeaKotlinModuleCoordinates(module),
        fragments = module.fragments.toList().map { fragment -> IdeaKotlinFragment(fragment) }
    )
}
