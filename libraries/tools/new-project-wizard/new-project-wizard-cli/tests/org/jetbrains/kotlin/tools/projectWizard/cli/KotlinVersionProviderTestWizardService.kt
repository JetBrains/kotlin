/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import org.jetbrains.kotlin.tools.projectWizard.core.service.EapVersionDownloader
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardKotlinVersion
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionKind
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionProviderService
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.BintrayRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository

class KotlinVersionProviderTestWizardService() : KotlinVersionProviderService(), TestWizardService {
    private val useCacheRedirector
        get() = System.getProperty("cacheRedirectorEnabled")?.toBoolean() == true


    override fun getKotlinVersion(projectKind: ProjectKind): WizardKotlinVersion =
        kotlinVersionWithDefaultValues(TEST_KOTLIN_VERSION)

    override fun getKotlinVersionRepository(versionKind: KotlinVersionKind): Repository = if (useCacheRedirector) {
        getKotlinVersionRepositoryWithCacheRedirector(versionKind)
    } else {
        super.getKotlinVersionRepository(versionKind)
    }

    private fun getKotlinVersionRepositoryWithCacheRedirector(versionKind: KotlinVersionKind): Repository = when (versionKind) {
        KotlinVersionKind.STABLE -> DefaultRepository.MAVEN_CENTRAL
        KotlinVersionKind.EAP -> KOTLIN_EAP_BINTRAY_WITH_CACHE_REDIRECTOR
        KotlinVersionKind.DEV -> KOTLIN_DEV_BINTRAY_WITH_CACHE_REDIRECTOR
        KotlinVersionKind.M -> KOTLIN_EAP_BINTRAY_WITH_CACHE_REDIRECTOR
    }

    companion object {
        private const val CACHE_REDIRECTOR_BINTRAY_URL = "https://cache-redirector.jetbrains.com/dl.bintray.com"

        val KOTLIN_EAP_BINTRAY_WITH_CACHE_REDIRECTOR = BintrayRepository("kotlin/kotlin-eap", CACHE_REDIRECTOR_BINTRAY_URL)
        val KOTLIN_DEV_BINTRAY_WITH_CACHE_REDIRECTOR = BintrayRepository("kotlin/kotlin-dev", CACHE_REDIRECTOR_BINTRAY_URL)


        val TEST_KOTLIN_VERSION by lazy {
            EapVersionDownloader.getLatestEapVersion()!!
        }
    }
}