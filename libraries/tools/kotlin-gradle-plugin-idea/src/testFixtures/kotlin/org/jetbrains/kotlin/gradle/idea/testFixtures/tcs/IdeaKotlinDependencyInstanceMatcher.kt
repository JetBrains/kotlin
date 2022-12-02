/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency

internal class IdeaKotlinDependencyInstanceMatcher(private val instance: IdeaKotlinDependency) : IdeaKotlinDependencyMatcher {
    override val description: String
        get() = instance.toString()

    override fun matches(dependency: IdeaKotlinDependency): Boolean {
        return instance == dependency
    }
}
