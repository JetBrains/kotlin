package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType

interface BuildSystemWizardService : WizardService {
    fun isSuitableFor(buildSystemType: BuildSystemType): Boolean
}