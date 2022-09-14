/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.io.Serializable

sealed interface IdeaKpmModuleCoordinates : Serializable {
    val buildId: String
    val projectPath: String
    val projectName: String
    val moduleName: String
    val moduleClassifier: String?
}

val IdeaKpmModuleCoordinates.path: String
    get() = "${buildId.takeIf { it != ":" }.orEmpty()}$projectPath/$moduleName"

@InternalKotlinGradlePluginApi
data class IdeaKpmModuleCoordinatesImpl(
    override val buildId: String,
    override val projectPath: String,
    override val projectName: String,
    override val moduleName: String,
    override val moduleClassifier: String?
) : IdeaKpmModuleCoordinates {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
