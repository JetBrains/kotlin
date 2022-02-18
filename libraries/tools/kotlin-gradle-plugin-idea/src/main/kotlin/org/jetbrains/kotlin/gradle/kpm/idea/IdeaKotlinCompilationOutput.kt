/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.File
import java.io.Serializable

interface IdeaKotlinCompilationOutput : Serializable {
    val classesDirs: Set<File>
    val resourcesDir: File?
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinCompilationOutputImpl(
    override val classesDirs: Set<File>,
    override val resourcesDir: File?
) : IdeaKotlinCompilationOutput {
    @InternalKotlinGradlePluginApi
    companion object {
        const val serialVersionUID = 0L
    }
}
