/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.service.EapVersionDownloader
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardKotlinVersion
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionKind
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionProviderService
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*

class KotlinVersionProviderTestWizardService() : KotlinVersionProviderService(), TestWizardService {
    private val useCacheRedirector
        get() = System.getProperty("cacheRedirectorEnabled")?.toBoolean() == true


    override fun getKotlinVersion(projectKind: ProjectKind): WizardKotlinVersion =
        kotlinVersionWithDefaultValues(
            when (projectKind) {
                ProjectKind.COMPOSE -> Versions.KOTLIN_VERSION_FOR_COMPOSE
                else -> TEST_KOTLIN_VERSION
            }
        )

    override fun getDevVersionRepository(): Repository =
        if (useCacheRedirector) KOTLIN_DEV_BINTRAY_WITH_CACHE_REDIRECTOR
        else super.getDevVersionRepository()

    companion object {
        private const val CACHE_REDIRECTOR_JETBRAINS_SPACE_URL = "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space"

        val KOTLIN_DEV_BINTRAY_WITH_CACHE_REDIRECTOR = CustomMavenRepositoryImpl("kotlin/p/kotlin/dev", CACHE_REDIRECTOR_JETBRAINS_SPACE_URL)

        val TEST_KOTLIN_VERSION by lazy {
            EapVersionDownloader.getLatestEapVersion()!!
        }
    }
}