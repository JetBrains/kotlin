/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.model

import java.io.File

data class PSourceSet(
    val name: String,
    val forTests: Boolean,
    val sourceDirectories: List<File>,
    val resourceDirectories: List<File>,
    val kotlinOptions: PSourceRootKotlinOptions?,
    val compileClasspathConfigurationName: String,
    val runtimeClasspathConfigurationName: String
)