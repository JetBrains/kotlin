/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.ComposeCompiler
import java.io.Serializable

/**
 * Implementation of the [ComposeCompiler] interface.
 */
data class ComposeCompilerImpl(
    override val name: String,
) : ComposeCompiler, Serializable {

    override val modelVersion = serialVersionUID

    companion object {
        private const val serialVersionUID = 1L
    }
}