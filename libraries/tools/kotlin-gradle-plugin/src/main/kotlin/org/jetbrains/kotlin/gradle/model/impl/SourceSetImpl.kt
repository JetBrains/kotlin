/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.CompilerArguments
import org.jetbrains.kotlin.gradle.model.SourceSet
import java.io.File
import java.io.Serializable

/**
 * Implementation of the [SourceSet] interface.
 */
data class SourceSetImpl(
    override val name: String,
    override val type: SourceSet.SourceSetType,
    override val friendSourceSets: Collection<String>,
    override val sourceDirectories: Collection<File>,
    override val resourcesDirectories: Collection<File>,
    override val classesOutputDirectory: File,
    override val resourcesOutputDirectory: File,
    override val compilerArguments: CompilerArguments
) : SourceSet, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}