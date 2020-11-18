/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.ide.impl.NewProjectUtil
import com.intellij.ide.projectWizard.NewProjectWizard
import com.intellij.ide.projectWizard.ProjectTypeStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import org.jetbrains.kotlin.idea.projectWizard.WizardStatsService
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object NewWizardOpener {
    fun open(template: ProjectTemplate?) {
        val task = object : Task.Backgroundable(
            null,
            KotlinNewProjectWizardUIBundle.message("project.opener.initialisation")
        ) {
            val wizard = NewProjectWizard(null, ModulesProvider.EMPTY_MODULES_PROVIDER, null)

            override fun run(indicator: ProgressIndicator) {
                // warm-up components, stolen from com.intellij.ide.impl.NewProjectUtil.createNewProject
                ProjectManager.getInstance().defaultProject
            }

            override fun onFinished() = ApplicationManager.getApplication().invokeLater {
                val step = wizard.currentStepObject as ProjectTypeStep
                step.setSelectedTemplate(KotlinNewProjectWizardUIBundle.message("generator.title"), null)
                if (template != null) {
                    wizard.projectBuilder
                        .safeAs<NewProjectWizardModuleBuilder>()
                        ?.selectProjectTemplate(template)
                }
                if (wizard.showAndGet()) {
                    val project: Project? = NewProjectUtil.createFromWizard(wizard, null)
                    WizardStatsService.logWizardOpenByHyperlink(project, template?.id)
                }
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(
            task,
            BackgroundableProcessIndicator(task)
        )
    }
}