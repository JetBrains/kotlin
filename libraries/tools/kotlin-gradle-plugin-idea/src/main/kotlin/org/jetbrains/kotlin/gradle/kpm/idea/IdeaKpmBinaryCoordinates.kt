/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

sealed interface IdeaKpmBinaryCoordinates : IdeaKpmDependencyCoordinates {
    val group: String
    val module: String
    val version: String
    val kotlinModuleName: String?
    val kotlinFragmentName: String?
}

@InternalKotlinGradlePluginApi
data class IdeaKpmBinaryCoordinatesImpl(
    override val group: String,
    override val module: String,
    override val version: String,
    override val kotlinModuleName: String? = null,
    override val kotlinFragmentName: String? = null
) : IdeaKpmBinaryCoordinates {

    override fun toString(): String {
        return "$group:$module:$version" +
                (if (kotlinModuleName != null) ":$kotlinModuleName" else "") +
                (if (kotlinFragmentName != null) ":$kotlinFragmentName" else "")
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}
