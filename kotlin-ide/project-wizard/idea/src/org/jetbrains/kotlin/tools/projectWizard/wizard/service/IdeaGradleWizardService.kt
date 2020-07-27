/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.UNIT_SUCCESS
import org.jetbrains.kotlin.tools.projectWizard.core.service.ProjectImportingWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.plugins.gradle.service.project.open.linkAndRefreshGradleProject
import java.nio.file.Path

// FIX ME WHEN BUNCH 201 REMOVED
class IdeaGradleWizardService(private val project: Project) : ProjectImportingWizardService,
                                                              IdeaWizardService {
    override fun isSuitableFor(buildSystemType: BuildSystemType): Boolean = buildSystemType.isGradle

    override fun importProject(
        reader: Reader,
        path: Path,
        modulesIrs: List<ModuleIR>,
        buildSystem: BuildSystemType
    ): TaskResult<Unit> {
        withGradleWrapperEnabled {
            linkAndRefreshGradleProject(path.toString(), project)
        }
        return UNIT_SUCCESS
    }

    private fun withGradleWrapperEnabled(action: () -> Unit) {
        val oldGradleDistributionType = System.getProperty("idea.gradle.distributionType")
        System.setProperty("idea.gradle.distributionType", "WRAPPED")
        try {
            action()
        } finally {
            if (oldGradleDistributionType != null) {
                System.setProperty("idea.gradle.distributionType", oldGradleDistributionType)
            }
        }
    }
}