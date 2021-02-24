/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.service

interface SettingSavingWizardService : WizardService {
    fun saveSettingValue(settingPath: String, settingValue: String)
    fun getSettingValue(settingPath: String): String?
}

class SettingSavingWizardServiceImpl : SettingSavingWizardService, IdeaIndependentWizardService {
    override fun saveSettingValue(settingPath: String, settingValue: String) {}
    override fun getSettingValue(settingPath: String): String? = null
}