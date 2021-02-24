/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.utils.ArchiveOperationsCompat
import org.jetbrains.kotlin.gradle.utils.FileSystemOperationsCompat
import java.io.File

/**
 * Cache for storing already created [GradleNodeModule]s
 */
internal class GradleNodeModulesCache(nodeJs: NodeJsRootExtension) : AbstractNodeModulesCache(nodeJs) {
    @Transient
    private val project = nodeJs.rootProject

    private val fs = FileSystemOperationsCompat(project)
    private val archiveOperations = ArchiveOperationsCompat(project)

    override fun buildImportedPackage(
        name: String,
        version: String,
        file: File
    ): File? {
        val module = GradleNodeModuleBuilder(fs, archiveOperations, name, version, listOf(file), dir)
        module.visitArtifacts()
        return module.rebuild()
    }
}