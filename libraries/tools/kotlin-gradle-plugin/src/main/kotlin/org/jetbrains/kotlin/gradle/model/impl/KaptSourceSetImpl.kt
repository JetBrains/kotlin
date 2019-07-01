/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.KaptSourceSet
import java.io.File
import java.io.Serializable

/**
 * Implementation of the [KaptSourceSet] interface.
 */
data class KaptSourceSetImpl(
    override val name: String,
    override val type: KaptSourceSet.KaptSourceSetType,
    override val generatedSourcesDirectory: File,
    override val generatedKotlinSourcesDirectory: File,
    override val generatedClassesDirectory: File
) : KaptSourceSet, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}