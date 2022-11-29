/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragmentCoordinates
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragmentCoordinatesImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment

internal fun IdeaKpmFragmentCoordinates(fragment: GradleKpmFragment): IdeaKpmFragmentCoordinates {
    return IdeaKpmFragmentCoordinatesImpl(
        module = IdeaKpmModuleCoordinates(fragment.containingModule),
        fragmentName = fragment.name
    )
}
