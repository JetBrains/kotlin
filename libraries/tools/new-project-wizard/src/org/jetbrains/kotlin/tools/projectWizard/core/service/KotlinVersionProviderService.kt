/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.service

import com.google.gson.JsonObject
import com.google.gson.JsonParser.parseString
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.asNullable
import org.jetbrains.kotlin.tools.projectWizard.core.compute
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.stream.Collectors

data class WizardKotlinVersion(
    val version: Version,
    val kind: KotlinVersionKind,
    val repository: Repository,
    val buildSystemPluginRepository: (BuildSystemType) -> Repository?,
)

abstract class KotlinVersionProviderService : WizardService {
    abstract fun getKotlinVersion(projectKind: ProjectKind): WizardKotlinVersion

    protected fun kotlinVersionWithDefaultValues(version: Version) = WizardKotlinVersion(
        version,
        getKotlinVersionKind(version),
        getKotlinVersionRepository(version),
        getBuildSystemPluginRepository(getKotlinVersionKind(version), getDevVersionRepository()),
    )


    private fun getKotlinVersionRepository(versionKind: KotlinVersionKind): Repository = when (versionKind) {
        KotlinVersionKind.STABLE, KotlinVersionKind.EAP, KotlinVersionKind.M -> DefaultRepository.MAVEN_CENTRAL
        KotlinVersionKind.DEV -> getDevVersionRepository()
    }

    protected open fun getDevVersionRepository(): Repository = Repositories.JETBRAINS_KOTLIN_DEV

    private fun getKotlinVersionRepository(version: Version) =
        getKotlinVersionRepository(getKotlinVersionKind(version))


    private fun getKotlinVersionKind(version: Version) = when {
        "eap" in version.toString().toLowerCase() -> KotlinVersionKind.EAP
        "rc" in version.toString().toLowerCase() -> KotlinVersionKind.EAP
        "dev" in version.toString().toLowerCase() -> KotlinVersionKind.DEV
        "m" in version.toString().toLowerCase() -> KotlinVersionKind.M
        else -> KotlinVersionKind.STABLE


    }

    companion object {
        fun getBuildSystemPluginRepository(
            versionKind: KotlinVersionKind,
            devRepository: Repository
        ): (BuildSystemType) -> Repository? =
            when (versionKind) {
                KotlinVersionKind.STABLE, KotlinVersionKind.EAP, KotlinVersionKind.M -> { buildSystem ->
                    when (buildSystem) {
                        BuildSystemType.GradleKotlinDsl, BuildSystemType.GradleGroovyDsl -> DefaultRepository.GRADLE_PLUGIN_PORTAL
                        BuildSystemType.Maven -> DefaultRepository.MAVEN_CENTRAL
                        BuildSystemType.Jps -> null
                    }
                }
                KotlinVersionKind.DEV -> { _ -> devRepository }
            }
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
    fun getLatestEapVersion() = downloadVersionFromMavenCentral(EAP_URL).firstOrNull()
    fun getLatestDevVersion() = downloadVersions(DEV_URL).firstOrNull()

    private fun downloadPage(url: String): TaskResult<String> = safe {
        BufferedReader(InputStreamReader(URL(url).openStream())).lines().collect(Collectors.joining("\n"))
    }

    @Suppress("SameParameterValue")
    private fun downloadVersionFromMavenCentral(url: String) = compute {
        val (text) = downloadPage(url)
        val (versionString) = parseLatestVersionFromJson(text)
        if (versionString.isNotEmpty())
            listOf(Version.fromString(versionString))
        else
            emptyList()
    }.asNullable.orEmpty()

    private fun parseLatestVersionFromJson(text: String) = safe {
        val json = parseString(text) as JsonObject
        json.get("response").asJsonObject.get("docs").asJsonArray.get(0).asJsonObject.get("latestVersion").asString
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
    private val EAP_URL = "https://search.maven.org/solrsearch/select?q=g:org.jetbrains.kotlin%20AND%20a:kotlin-gradle-plugin"

    @NonNls
    private val DEV_URL = "https://dl.bintray.com/kotlin/kotlin-dev/org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/"

    @NonNls
    private val versionRegexp = """href="([^"\\]+)"""".toRegex()
}