/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import java.io.File

/**
 * Cache for storing already created [GradleNodeModule]s
 */
internal class GradleNodeModulesCache(nodeJs: NodeJsRootExtension) : AbstractNodeModulesCache(nodeJs) {
    private val fs = (nodeJs.rootProject as ProjectInternal).services.get(FileSystemOperations::class.java)
    private val archiveOperations = (nodeJs.rootProject as ProjectInternal).services.get(ArchiveOperations::class.java)

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