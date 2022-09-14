/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.io.File
import java.io.Serializable

sealed interface IdeaKpmProject : Serializable {
    val gradlePluginVersion: String
    val coreLibrariesVersion: String
    val explicitApiModeCliOption: String?
    val kotlinNativeHome: File
    val modules: List<IdeaKpmModule>
}

@InternalKotlinGradlePluginApi
data class IdeaKpmProjectImpl(
    override val gradlePluginVersion: String,
    override val coreLibrariesVersion: String,
    override val explicitApiModeCliOption: String?,
    override val kotlinNativeHome: File,
    override val modules: List<IdeaKpmModule>
) : IdeaKpmProject {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
