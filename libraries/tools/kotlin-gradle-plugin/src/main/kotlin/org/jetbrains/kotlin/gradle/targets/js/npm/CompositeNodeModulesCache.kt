/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import java.io.File

/**
 * Cache for storing already created [GradleNodeModule]s
 */
internal class CompositeNodeModulesCache(nodeJs: NodeJsRootExtension) : AbstractNodeModulesCache(nodeJs) {
    override fun buildImportedPackage(
        name: String,
        version: String,
        file: File
    ): File? {
        val module = CompositeNodeModuleBuilder(file, dir)
        return module.rebuild()
    }
}