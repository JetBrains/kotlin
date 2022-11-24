/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectArtifactDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency

internal class IdeaKotlinProjectArtifactDependencyMatcher(
    val type: IdeaKotlinSourceDependency.Type,
    val projectPath: String,
    val artifactFilePath: Regex
) : IdeaKotlinDependencyMatcher {
    override val description: String
        get() = "project($type)::$projectPath/${artifactFilePath.pattern}"

    override fun matches(dependency: IdeaKotlinDependency): Boolean {
        if (dependency !is IdeaKotlinProjectArtifactDependency) return false
        return dependency.type == type &&
                dependency.coordinates.project.projectPath == projectPath &&
                dependency.coordinates.artifactFile.path.matches(artifactFilePath)

    }
}