/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.openapi.application.JBProtocolCommand
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate

class OpenNewProjectWizardProtocolCommand : JBProtocolCommand(COMMAND_NAME) {
    override fun perform(target: String?, parameters: Map<String, String?>) {
        when (target) {
            NEW_PROJECT_TARGET -> showCreateNewProjectWizard(parameters)
        }
    }

    private fun showCreateNewProjectWizard(parameters: Map<String, String?>) {
        val template = parameters[NEW_PROJECT_TARGET_TEMPLATE_PARAMETER]
            ?.let(ProjectTemplate.Companion::byId)

        NewWizardOpener.open(template)
    }

    companion object {
        private const val COMMAND_NAME = "kotlin-wizard"
        private const val NEW_PROJECT_TARGET = "create-project"
        private const val NEW_PROJECT_TARGET_TEMPLATE_PARAMETER = "template"
    }
}