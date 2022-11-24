/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import java.io.File
import java.io.Serializable

sealed interface IdeaKotlinDependencyCoordinates : Serializable

data class IdeaKotlinBinaryCoordinates(
    val group: String,
    val module: String,
    val version: String?,
    val sourceSetName: String? = null,
) : IdeaKotlinDependencyCoordinates {
    override fun toString(): String {
        return "$group:$module:$version${sourceSetName?.let { ":$it" }.orEmpty()}"
    }

    internal companion object {
        const val serialVersionUID = 0L
    }
}

data class IdeaKotlinSourceCoordinates(
    val project: IdeaKotlinProjectCoordinates,
    val sourceSetName: String
) : IdeaKotlinDependencyCoordinates {

    val buildId: String get() = project.buildId
    val projectPath: String get() = project.projectPath
    val projectName: String get() = project.projectName

    override fun toString(): String {
        return "$project/$sourceSetName"
    }

    internal companion object {
        const val serialVersionUID = 0L
    }
}

data class IdeaKotlinProjectArtifactCoordinates(
    val project: IdeaKotlinProjectCoordinates,
    val artifactFile: File
) : IdeaKotlinDependencyCoordinates {

    internal companion object {
        const val serialVersionUID = 0L
    }
}
