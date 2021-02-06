package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.computeM
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import java.nio.file.Files
import java.nio.file.Path

interface FileSystemWizardService : WizardService {
    fun createFile(path: Path, text: String): TaskResult<Unit>
    fun createDirectory(path: Path): TaskResult<Unit>
}

class OsFileSystemWizardService : FileSystemWizardService, IdeaIndependentWizardService {
    override fun createFile(path: Path, text: String) = computeM {
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