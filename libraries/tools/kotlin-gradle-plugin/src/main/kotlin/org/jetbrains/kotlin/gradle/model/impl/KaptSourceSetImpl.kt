/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.KaptSourceSet
import java.io.File
import java.io.Serializable

/**
 * Implementation of the [KaptSourceSet] interface.
 */
data class KaptSourceSetImpl(
    private val myName: String,
    private val myType: KaptSourceSet.KaptSourceSetType,
    private val myGeneratedSourcesDirectory: File,
    private val myGeneratedKotlinSourcesDirectory: File,
    private val myGeneratedClassesDirectory: File
) : KaptSourceSet, Serializable {

    override fun getName(): String {
        return myName
    }

    override fun getType(): KaptSourceSet.KaptSourceSetType {
        return myType
    }

    override fun getGeneratedSourcesDirectory(): File {
        return myGeneratedSourcesDirectory
    }

    override fun getGeneratedKotlinSourcesDirectory(): File {
        return myGeneratedKotlinSourcesDirectory
    }

    override fun getGeneratedClassesDirectory(): File {
        return myGeneratedClassesDirectory
    }
}