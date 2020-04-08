/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.ide.util.PropertiesComponent
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.tools.projectWizard.core.service.SettingSavingWizardService

class IdeaSettingSavingWizardService : SettingSavingWizardService, IdeaWizardService {
    override fun saveSettingValue(settingPath: String, settingValue: String) = runWriteAction {
        PropertiesComponent.getInstance().setValue(PATH_PREFIX + settingPath, settingValue)
    }

    override fun getSettingValue(settingPath: String): String? = runReadAction {
        PropertiesComponent.getInstance().getValue(PATH_PREFIX + settingPath)
    }

    companion object {
        private const val PATH_PREFIX = "NewKotlinWizard."
    }
}