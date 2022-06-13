/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.emptyExtras
import java.io.File
import java.io.Serializable

sealed interface IdeaKpmSourceDirectory : Serializable {
    val extras: Extras
    val file: File
    val type: String

    companion object {
        const val SOURCE_TYPE = "source"
        const val RESOURCE_TYPE = "resource"
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKpmSourceDirectoryImpl(
    override val file: File,
    override val type: String,
    override val extras: Extras = emptyExtras(),
    ) : IdeaKpmSourceDirectory {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
