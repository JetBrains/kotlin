/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

sealed interface IdeaKotlinFragmentCoordinates : Serializable, IdeaKotlinDependencyCoordinates {
    val module: IdeaKotlinModuleCoordinates
    val fragmentName: String
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinFragmentCoordinatesImpl(
    override val module: IdeaKotlinModuleCoordinates,
    override val fragmentName: String
) : IdeaKotlinFragmentCoordinates {

    override fun toString(): String = path

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

val IdeaKotlinFragmentCoordinates.path: String
    get() = "${module.path}/$fragmentName"
