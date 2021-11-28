/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.file.FileSystemOperations
import org.jetbrains.kotlin.gradle.utils.ArchiveOperationsCompat
import org.jetbrains.kotlin.gradle.utils.FileSystemOperationsCompat
import java.io.File
import javax.inject.Inject

/**
 * Cache for storing already created [GradleNodeModule]s
 */
internal abstract class GradleNodeModulesCache : AbstractNodeModulesCache() {

    // TODO: replace by injected service org.gradle.api.file.FileSystemOperations once min support Gradle is 6.2
    // https://github.com/gradle/gradle/commit/d02b9d84c08dba64775fb9581e3280f88d319a21
    @Transient
    lateinit var fs: FileSystemOperationsCompat

    override val type: String
        get() = "gradle"

    // TODO: replace by injected service org.gradle.api.file.ArchiveOperations once min supported Gradle is 6.6
    @Transient
    lateinit var archiveOperations: ArchiveOperationsCompat

    override fun buildImportedPackage(
        name: String,
        version: String,
        file: File
    ): File? {
        val module = GradleNodeModuleBuilder(fs, archiveOperations, name, version, listOf(file), parameters.cacheDir.get().asFile)
        module.visitArtifacts()
        return module.rebuild()
    }
}