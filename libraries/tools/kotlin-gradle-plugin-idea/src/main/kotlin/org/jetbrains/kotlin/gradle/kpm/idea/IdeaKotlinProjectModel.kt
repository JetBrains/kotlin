/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.File
import java.io.Serializable

interface IdeaKotlinProjectModel : Serializable {
    val gradlePluginVersion: String
    val coreLibrariesVersion: String
    val explicitApiModeCliOption: String?
    val kotlinNativeHome: File
    val modules: List<IdeaKotlinModule>
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinProjectModelImpl(
    override val gradlePluginVersion: String,
    override val coreLibrariesVersion: String,
    override val explicitApiModeCliOption: String?,
    override val kotlinNativeHome: File,
    override val modules: List<IdeaKotlinModule>
) : IdeaKotlinProjectModel {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
