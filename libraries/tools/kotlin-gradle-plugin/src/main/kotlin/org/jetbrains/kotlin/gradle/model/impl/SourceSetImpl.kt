/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
    private val myName: String,
    private val myType: SourceSet.SourceSetType,
    private val myFriendSourceSets: Collection<String>,
    private val mySourceDirectories: Collection<File>,
    private val myResourcesDirectories: Collection<File>,
    private val myClassesOutputDirectory: File,
    private val myResourcesOutputDirectory: File,
    private val myCompilerArguments: CompilerArguments
) : SourceSet, Serializable {

    override fun getName(): String {
        return myName
    }

    override fun getType(): SourceSet.SourceSetType {
        return myType
    }

    override fun getFriendSourceSets(): Collection<String> {
        return myFriendSourceSets
    }

    override fun getSourceDirectories(): Collection<File> {
        return mySourceDirectories
    }

    override fun getResourcesDirectories(): Collection<File> {
        return myResourcesDirectories
    }

    override fun getClassesOutputDirectory(): File {
        return myClassesOutputDirectory
    }

    override fun getResourcesOutputDirectory(): File {
        return myResourcesOutputDirectory
    }

    override fun getCompilerArguments(): CompilerArguments {
        return myCompilerArguments
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}