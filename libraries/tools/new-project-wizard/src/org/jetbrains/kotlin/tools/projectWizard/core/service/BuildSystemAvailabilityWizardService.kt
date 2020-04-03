/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.service


import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType

interface BuildSystemAvailabilityWizardService : WizardService {
    fun isAvailable(buildSystemType: BuildSystemType): Boolean
}

fun Reader.isBuildSystemAvailable(buildSystemType: BuildSystemType) =
    service<BuildSystemAvailabilityWizardService>().isAvailable(buildSystemType)

class BuildSystemAvailabilityWizardServiceImpl : BuildSystemAvailabilityWizardService, IdeaIndependentWizardService {
    override fun isAvailable(buildSystemType: BuildSystemType) = true
}