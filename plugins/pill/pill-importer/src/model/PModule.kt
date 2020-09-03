/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.model

import org.jetbrains.kotlin.pill.GradleProjectPath
import java.io.File

data class PModule(
    val name: String,
    val path: GradleProjectPath,
    val forTests: Boolean,
    val rootDirectory: File,
    val moduleFile: File,
    val contentRoots: List<PContentRoot>,
    val orderRoots: List<POrderRoot>,
    val kotlinOptions: PSourceRootKotlinOptions?,
    val moduleForProductionSources: PModule? = null,
    val embeddedDependencies: List<PDependency>
)

data class PContentRoot(
    val path: File,
    val sourceRoots: List<PSourceRoot>,
    val excludedDirectories: List<File>
)

data class PSourceRoot(val directory: File, val kind: Kind) {
    enum class Kind { PRODUCTION, TEST, RESOURCES, TEST_RESOURCES }
}

data class PSourceRootKotlinOptions(
    val noStdlib: Boolean?,
    val noReflect: Boolean?,
    val moduleName: String?,
    val apiVersion: String?,
    val languageVersion: String?,
    val jvmTarget: String?,
    val extraArguments: List<String>
)