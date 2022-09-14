/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmContentRoot.Companion.RESOURCES_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmContentRoot.Companion.SOURCES_TYPE
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.emptyExtras
import java.io.File
import java.io.Serializable

sealed interface IdeaKpmContentRoot : Serializable {
    val extras: Extras
    val file: File
    val type: String

    companion object {
        const val SOURCES_TYPE = "source"
        const val RESOURCES_TYPE = "resource"
    }
}

val IdeaKpmContentRoot.isSources get() = type == SOURCES_TYPE

val IdeaKpmContentRoot.isResources get() = type == RESOURCES_TYPE

@InternalKotlinGradlePluginApi
data class IdeaKpmContentRootImpl(
    override val file: File,
    override val type: String,
    override val extras: Extras = emptyExtras(),
) : IdeaKpmContentRoot {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
