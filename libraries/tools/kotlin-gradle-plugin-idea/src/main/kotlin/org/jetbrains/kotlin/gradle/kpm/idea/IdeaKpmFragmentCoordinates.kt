/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.io.Serializable

sealed interface IdeaKpmFragmentCoordinates : Serializable, IdeaKpmDependencyCoordinates {
    val module: IdeaKpmModuleCoordinates
    val fragmentName: String
}

@InternalKotlinGradlePluginApi
data class IdeaKpmFragmentCoordinatesImpl(
    override val module: IdeaKpmModuleCoordinates,
    override val fragmentName: String
) : IdeaKpmFragmentCoordinates {

    override fun toString(): String = path

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

val IdeaKpmFragmentCoordinates.path: String
    get() = "${module.path}/$fragmentName"
