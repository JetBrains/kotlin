/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.CompilerArguments
import java.io.File
import java.io.Serializable

/**
 * Implementation of the [CompilerArguments] interface.
 */
data class CompilerArgumentsImpl(
    override val currentArguments: List<String>,
    override val defaultArguments: List<String>,
    override val compileClasspath: List<File>
) : CompilerArguments, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}