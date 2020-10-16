/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.asNullable
import org.jetbrains.kotlin.tools.projectWizard.core.compute
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.stream.Collectors

data class WizardKotlinVersion(val version: Version, val kind: KotlinVersionKind, val repository: Repository)

abstract class KotlinVersionProviderService : WizardService {
    abstract fun getKotlinVersion(projectKind: ProjectKind): WizardKotlinVersion

    protected fun kotlinVersionWithDefaultValues(version: Version) = WizardKotlinVersion(
        version,
        getKotlinVersionKind(version),
        getKotlinVersionRepository(version)
    )

    protected open fun getKotlinVersionRepository(versionKind: KotlinVersionKind): Repository = when (versionKind) {
        KotlinVersionKind.STABLE -> DefaultRepository.MAVEN_CENTRAL
        KotlinVersionKind.EAP -> Repositories.KOTLIN_EAP_BINTRAY
        KotlinVersionKind.M -> Repositories.KOTLIN_EAP_BINTRAY
        KotlinVersionKind.DEV -> Repositories.KOTLIN_DEV_BINTRAY
    }

    private fun getKotlinVersionRepository(version: Version) =
        getKotlinVersionRepository(getKotlinVersionKind(version))

    private fun getKotlinVersionKind(version: Version) = when {
        "eap" in version.toString().toLowerCase() -> KotlinVersionKind.EAP
        "rc" in version.toString().toLowerCase() -> KotlinVersionKind.EAP
        "dev" in version.toString().toLowerCase() -> KotlinVersionKind.DEV
        "m" in version.toString().toLowerCase() -> KotlinVersionKind.M
        else -> KotlinVersionKind.STABLE
    }
}


class KotlinVersionProviderServiceImpl : KotlinVersionProviderService(), IdeaIndependentWizardService {
    override fun getKotlinVersion(projectKind: ProjectKind): WizardKotlinVersion =
        kotlinVersionWithDefaultValues(Versions.KOTLIN)
}


enum class KotlinVersionKind {
    STABLE,
    EAP,
    DEV,
    M
}

val KotlinVersionKind.isStable
    get() = this == KotlinVersionKind.STABLE

object EapVersionDownloader {
    fun getLatestEapVersion() = downloadVersions(EAP_URL).firstOrNull()
    fun getLatestDevVersion() = downloadVersions(DEV_URL).firstOrNull()

    private fun downloadPage(url: String): TaskResult<String> = safe {
        BufferedReader(InputStreamReader(URL(url).openStream())).lines().collect(Collectors.joining("\n"))
    }

    @Suppress("SameParameterValue")
    private fun downloadVersions(url: String): List<Version> = compute {
        val (text) = downloadPage(url)
        versionRegexp.findAll(text)
            .map { it.groupValues[1].removeSuffix("/") }
            .filter { it.isNotEmpty() && it[0].isDigit() }
            .map { Version.fromString(it) }
            .toList()
            .asReversed()
    }.asNullable.orEmpty()

    @NonNls
    private val EAP_URL = "https://dl.bintray.com/kotlin/kotlin-eap/org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/"

    @NonNls
    private val DEV_URL = "https://dl.bintray.com/kotlin/kotlin-dev/org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/"

    @NonNls
    private val versionRegexp = """href="([^"\\]+)"""".toRegex()
}