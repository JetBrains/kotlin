/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.services

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.tools.projectWizard.cli.KotlinVersionProviderTestWizardService
import org.jetbrains.kotlin.tools.projectWizard.cli.TestWizardService

object TestWizardServices {
    fun createProjectDependent(project: Project): List<TestWizardService> = listOf(
        GradleProjectImportingTestWizardService(project)
    )

    val PROJECT_INDEPENDENT = listOf(
        FormattingTestWizardService(),
        KotlinVersionProviderTestWizardService()
    )
}