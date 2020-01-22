/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.jetbrains.kotlin.tools.projectWizard.WizardRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext

interface RunConfigurationsService : WizardService {
    fun ValuesReadingContext.addRunConfigurations(configurations: List<WizardRunConfiguration>)
}

class RunConfigurationsServiceImpl : RunConfigurationsService, IdeaIndependentWizardService {
    override fun ValuesReadingContext.addRunConfigurations(configurations: List<WizardRunConfiguration>) = Unit
}