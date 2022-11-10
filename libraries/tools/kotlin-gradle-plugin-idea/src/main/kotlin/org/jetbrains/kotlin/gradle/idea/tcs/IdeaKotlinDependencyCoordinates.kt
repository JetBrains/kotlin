/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import java.io.Serializable

sealed interface IdeaKotlinDependencyCoordinates : Serializable

data class IdeaKotlinBinaryCoordinates(
    val group: String,
    val module: String,
    val version: String?,
    val sourceSetName: String?
) : IdeaKotlinDependencyCoordinates {
    override fun toString(): String {
        return "$group:$module:$version${sourceSetName?.let { ":$it" }.orEmpty()}"
    }

    internal companion object {
        const val serialVersionUID = 0L
    }

}

data class IdeaKotlinSourceCoordinates(
    val buildId: String,
    val projectPath: String,
    val projectName: String,
    val sourceSetName: String
) : IdeaKotlinDependencyCoordinates {
    override fun toString(): String {
        return "${buildId.takeIf { it != ":" }.orEmpty()}$projectPath/$sourceSetName"
    }

    internal companion object {
        const val serialVersionUID = 0L
    }
}
