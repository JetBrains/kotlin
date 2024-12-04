/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectArtifactDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.artifactsClasspath
import java.io.File

internal class IdeaKotlinProjectArtifactDependencyMatcher(
    val type: IdeaKotlinSourceDependency.Type,
    val buildPath: String,
    val projectPath: String,
    val artifactFilePath: FilePathRegex
) : IdeaKotlinDependencyMatcher {
    override val description: String
        get() = "project($type)::$projectPath/${artifactFilePath}"

    override fun matches(dependency: IdeaKotlinDependency): Boolean {
        if (dependency !is IdeaKotlinProjectArtifactDependency) return false
        return dependency.type == type &&
                dependency.coordinates.buildPath == buildPath &&
                dependency.coordinates.projectPath == projectPath &&
                dependency.artifactsClasspath.any { artifactFilePath.matches(it) }

    }
}


fun FilePathRegex(pattern: String): FilePathRegex = FilePathRegex.from(pattern)

class FilePathRegex private constructor(private val normalizedRegex: Regex) {
    override fun toString(): String {
        return normalizedRegex.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FilePathRegex) return false
        return normalizedRegex == other.normalizedRegex
    }

    override fun hashCode(): Int {
        return normalizedRegex.hashCode()
    }

    fun matches(file: File) = normalizedRegex.matches(file.path)

    fun matches(path: String) = normalizedRegex.matches(path)

    companion object {
        fun from(pattern: String): FilePathRegex {
            return FilePathRegex(Regex(pattern.replace("/", Regex.escape(File.separator))))
        }
    }
}
