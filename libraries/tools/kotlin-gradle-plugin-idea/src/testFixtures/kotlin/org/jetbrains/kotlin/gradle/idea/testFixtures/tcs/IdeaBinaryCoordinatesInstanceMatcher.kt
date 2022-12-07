/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency

internal class IdeaBinaryCoordinatesInstanceMatcher(
    private val coordinates: IdeaKotlinBinaryCoordinates
) : IdeaKotlinDependencyMatcher {
    override val description: String
        get() = "coordinates:$coordinates"

    override fun matches(dependency: IdeaKotlinDependency): Boolean {
        if (dependency !is IdeaKotlinBinaryDependency) return false
        return coordinates == dependency.coordinates
    }
}
