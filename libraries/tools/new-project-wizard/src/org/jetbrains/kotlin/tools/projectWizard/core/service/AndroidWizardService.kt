package org.jetbrains.kotlin.tools.projectWizard.core.service

import java.nio.file.Path

interface AndroidWizardService : WizardService {
    fun isValidAndroidSdk(path: Path): Boolean
}

class AndroidWizardServiceImpl : AndroidWizardService, IdeaIndependentWizardService {
    //TODO use some heuristics
    override fun isValidAndroidSdk(path: Path): Boolean = true
}