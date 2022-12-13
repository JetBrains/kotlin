/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency

interface IdeaKotlinDependencyMatcher {
    val description: String
    fun matches(dependency: IdeaKotlinDependency): Boolean
}

fun IdeaKotlinDependencyMatcher(description: String, matches: (dependency: IdeaKotlinDependency) -> Boolean) =
    object : IdeaKotlinDependencyMatcher {
        override val description: String = description
        override fun matches(dependency: IdeaKotlinDependency): Boolean = matches(dependency)
    }