/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.io.File
import java.io.Serializable

sealed interface IdeaKpmCompilationOutput : Serializable {
    val classesDirs: Set<File>
    val resourcesDir: File?
}

@InternalKotlinGradlePluginApi
data class IdeaKpmCompilationOutputImpl(
    override val classesDirs: Set<File>,
    override val resourcesDir: File?
) : IdeaKpmCompilationOutput {

    @InternalKotlinGradlePluginApi
    companion object {
        const val serialVersionUID = 0L
    }
}
