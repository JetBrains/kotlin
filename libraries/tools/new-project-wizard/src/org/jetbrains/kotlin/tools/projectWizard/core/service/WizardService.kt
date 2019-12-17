package org.jetbrains.kotlin.tools.projectWizard.core.service

interface WizardService

interface IdeaIndependentWizardService : WizardService

object Services {
    val IDEA_INDEPENDENT_SERVICES: List<IdeaIndependentWizardService> = listOf(
        ProjectImportingWizardServiceImpl(),
        OsFileSystemWizardService(),
        AndroidWizardServiceImpl(),
        BuildSystemAvailabilityWizardServiceImpl(),
        DummyFileFormattingService()
    )
}

