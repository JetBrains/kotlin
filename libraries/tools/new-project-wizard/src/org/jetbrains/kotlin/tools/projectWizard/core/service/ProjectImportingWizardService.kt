package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.UNIT_SUCCESS
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import java.nio.file.Path

interface ProjectImportingWizardService : BuildSystemWizardService {
    fun importProject(path: Path, modulesIrs: List<ModuleIR>): TaskResult<Unit>
}

class ProjectImportingWizardServiceImpl : ProjectImportingWizardService, IdeaIndependentWizardService {
    override fun isSuitableFor(buildSystemType: BuildSystemType): Boolean = true
    override fun importProject(path: Path, modulesIrs: List<ModuleIR>) = UNIT_SUCCESS
}
