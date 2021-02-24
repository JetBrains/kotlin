/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import org.jetbrains.kotlin.tools.projectWizard.core.service.Services
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

interface TestWizardService : WizardService {
    companion object {
        val SERVICES = listOf(
            KotlinVersionProviderTestWizardService()
        )
    }
}

val CLI_WIZARD_TEST_SERVICES_MANAGER = ServicesManager(
    Services.IDEA_INDEPENDENT_SERVICES + TestWizardService.SERVICES
) { services ->
    services.firstIsInstanceOrNull<TestWizardService>()
        ?: services.firstOrNull()
}
