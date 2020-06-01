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
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.stream.Collectors

interface KotlinVersionProviderService : WizardService {
    fun getKotlinVersion(): Version
}

class KotlinVersionProviderServiceImpl : KotlinVersionProviderService, IdeaIndependentWizardService {
    override fun getKotlinVersion(): Version = Versions.KOTLIN
}


val Version.kotlinVersionKind
    get() = when {
        "eap" in toString().toLowerCase() -> KotlinVersionKind.EAP
        "dev" in toString().toLowerCase() -> KotlinVersionKind.DEV
        "m" in toString().toLowerCase() -> KotlinVersionKind.M
        else -> KotlinVersionKind.STABLE
    }

enum class KotlinVersionKind(val repository: Repository) {
    STABLE(repository = DefaultRepository.MAVEN_CENTRAL),
    EAP(repository = Repositories.KOTLIN_EAP_BINTRAY),
    DEV(repository = Repositories.KOTLIN_DEV_BINTRAY),
    M(repository = Repositories.KOTLIN_EAP_BINTRAY)
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