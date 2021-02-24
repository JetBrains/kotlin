/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard

import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls

interface WizardRunConfiguration {
    @get:Nls
    val configurationName: String
}

data class WizardGradleRunConfiguration(
    @Nls override val configurationName: String,
    @NonNls val taskName: String,
    val parameters: List<String>
) : WizardRunConfiguration