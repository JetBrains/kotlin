/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import org.jetbrains.kotlin.tools.projectWizard.core.service.EapVersionDownloader
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionProviderService
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

class KotlinVersionProviderTestWizardService : KotlinVersionProviderService, TestWizardService {
    override fun getKotlinVersion(): Version = TEST_KOTLIN_VERSION

    companion object {
        val TEST_KOTLIN_VERSION by lazy {
            EapVersionDownloader.getLatestDevVersion()!!
        }
    }
}