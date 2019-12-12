package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.jetbrains.kotlin.tools.projectWizard.core.UNIT_SUCCESS
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import java.nio.file.Path

interface GradleService : BuildSystemService

class OsGradleIntegration : GradleService {
    override fun importProject(
        path: Path,
        modulesIrs: List<ModuleIR>
    ) = UNIT_SUCCESS
}