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
    private val myCurrentArguments: List<String>,
    private val myDefaultArguments: List<String>,
    private val myCompilerClasspath: List<File>
) : CompilerArguments, Serializable {

    override fun getCurrentArguments(): List<String> {
        return myCurrentArguments
    }

    override fun getDefaultArguments(): List<String> {
        return myDefaultArguments
    }

    override fun getCompileClasspath(): List<File> {
        return myCompilerClasspath
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}