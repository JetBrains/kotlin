/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.model

import org.jetbrains.kotlin.pill.GradleProjectPath
import org.jetbrains.kotlin.pill.OutputDir
import java.io.File

data class PProject(
    val name: String,
    val rootDirectory: File,
    val modules: List<PModule>,
    val libraries: List<PLibrary>,
    val artifacts: Map<OutputDir, List<GradleProjectPath>>
)