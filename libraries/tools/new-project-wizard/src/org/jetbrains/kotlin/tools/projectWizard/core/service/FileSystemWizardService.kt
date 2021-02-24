/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.jetbrains.kotlin.tools.projectWizard.core.Success
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.computeM
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

interface FileSystemWizardService : WizardService {
    fun createFile(path: Path, text: String): TaskResult<Unit>
    fun createDirectory(path: Path): TaskResult<Unit>

    fun renderPath(path: Path): String {
        return path.toString().replace("""\""", """\\""")
    }
}

class OsFileSystemWizardService : FileSystemWizardService, IdeaIndependentWizardService {
    override fun createFile(path: Path, text: String) = computeM {
        if (path.toFile().exists()) return@computeM Success(Unit)
        createDirectory(path.parent).ensure()
        safe { Files.createFile(path.normalize()).toFile().writeText(text) }
    }

    override fun createDirectory(path: Path) = safe {
        @Suppress("NAME_SHADOWING") val path = path.normalize()
        if (Files.notExists(path)) {
            Files.createDirectories(path)
        }
    }
}