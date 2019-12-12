package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import java.nio.file.Path

interface BuildSystemService : Service {
    fun importProject(
        path: Path,
        modulesIrs: List<ModuleIR>
    ): TaskResult<Unit>
}