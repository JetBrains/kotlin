/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.Serializable

/**
 * Consumer Task: Reads the JSON file and performs the validation checks,
 * reporting diagnostics if any mismatches are found.
 * This task has no outputs, ensuring it re-runs if the input JSON changes.
 */
@DisableCachingByDefault(because = "This task's output is a diagnostic, which should be re-evaluated on each build.")
internal abstract class CheckXcodeTargetsConfigurationTask : DefaultTask(), UsesKotlinToolingDiagnostics {
    init {
        onlyIf("Task can only run on macOS") { HostManager.hostIsMac }
    }

    companion object {
        const val TASK_NAME = "checkXcodeProjectConfiguration"
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val xcodeProjectPath: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val pbxprojJson: RegularFileProperty

    @get:Internal
    abstract val appleTargets: ListProperty<KonanTarget>

    @TaskAction
    internal fun checkXcodeTargets() {
        val pbxprojContent = pbxprojJson.getFile().readText()
        if (pbxprojContent.isBlank() || pbxprojContent == "{}") {
            // This can happen if the producer task failed. Error is already logged.
            return
        }

        val gson = Gson()
        val xcodeTargets = parseXcodeTargets(pbxprojContent, gson, logger) ?: return

        // We are only interested in Xcode targets that produce a runnable application,
        // as these are the targets that will consume the Kotlin framework.
        // These strings are Apple's official "Product Type Identifiers".
        val xcodeAppTargets = xcodeTargets.filter {
            // Standard identifier for iOS, macOS, and tvOS applications.
            it.productType == "com.apple.product-type.application" ||
                    // Special identifier for the iOS application that acts as a container for a watchOS app.
                    it.productType == "com.apple.product-type.application.watchapp2-container"
        }

        if (xcodeAppTargets.isEmpty()) {
            reportDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic(xcodeProjectPath.getFile()))
            return
        }

        val allXcodeAppPlatforms = xcodeAppTargets.flatMap { it.platforms }.toSet()

        val missingTargets = mutableListOf<KonanTarget>()
        for (kotlinTarget in appleTargets.get()) {
            val expectedSdkRoot = getExpectedSdkRoot(kotlinTarget)
            if (expectedSdkRoot != unknownSdkRoot && expectedSdkRoot !in allXcodeAppPlatforms) {
                missingTargets.add(kotlinTarget)
            }
        }

        if (missingTargets.isNotEmpty()) {
            reportDiagnostic(
                KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic(missingTargets, xcodeProjectPath.getFile())
            )
        }
    }

    private fun parseXcodeTargets(
        pbxprojContent: String,
        gson: Gson,
        logger: Logger,
    ): Set<XcodeProjectStructure.XcodeTarget>? {
        try {
            val pbxproj = gson.fromJson(pbxprojContent, XcodeProjectStructure.Pbxproj::class.java)
            val objects = pbxproj.objects

            val rootProjectJson = objects[pbxproj.rootObject] ?: return emptySet()
            val rootProject = gson.fromJson(rootProjectJson, XcodeProjectStructure.PbxProject::class.java)
            val projectConfigs = getConfigurationMap(objects, rootProject.buildConfigurationList, gson)

            return rootProject.targets.mapNotNull { targetId ->
                parseSingleNativeTarget(targetId, objects, projectConfigs, gson)
            }.toSet()

        } catch (exception: Exception) {
            logger.error(
                "Failed to parse the JSON file for '${xcodeProjectPath.getFile().path}'. The file might be malformed.",
                exception
            )
            return null
        }
    }

    private fun parseSingleNativeTarget(
        targetId: String,
        objects: Map<String, JsonObject>,
        projectConfigs: Map<String, String>,
        gson: Gson,
    ): XcodeProjectStructure.XcodeTarget? {
        val targetJson = objects[targetId] ?: return null
        if (targetJson.get("isa")?.asString != "PBXNativeTarget") return null

        val nativeTarget = gson.fromJson(targetJson, XcodeProjectStructure.PbxNativeTarget::class.java)
        val targetConfigs = getConfigurationMap(objects, nativeTarget.buildConfigurationList, gson)

        val platforms = mutableSetOf<String>()

        for ((configName, configId) in targetConfigs) {
            val targetConfig = gson.fromJson(objects[configId], XcodeProjectStructure.XCBuildConfiguration::class.java)
            val projectConfigId = projectConfigs[configName]
            val projectConfig =
                projectConfigId?.let { gson.fromJson(objects[it], XcodeProjectStructure.XCBuildConfiguration::class.java) }

            val targetSettings = targetConfig.buildSettings
            val projectSettings = projectConfig?.buildSettings ?: XcodeProjectStructure.BuildSettings()

            val supportedPlatforms = targetSettings.supportedPlatforms ?: projectSettings.supportedPlatforms
            if (supportedPlatforms != null) {
                supportedPlatforms.split(" ").forEach { sdk ->
                    if (sdk.isNotBlank()) {
                        platforms.add(sdk)
                    }
                }
            } else {
                val sdkRoot = targetSettings.sdkRoot ?: projectSettings.sdkRoot
                if (sdkRoot != null) {
                    platforms.add(sdkRoot)
                }
            }
        }

        return if (platforms.isNotEmpty()) {
            XcodeProjectStructure.XcodeTarget(nativeTarget.name, nativeTarget.productType, platforms)
        } else {
            null
        }
    }

    private fun getConfigurationMap(
        objects: Map<String, JsonObject>,
        listId: String,
        gson: Gson,
    ): Map<String, String> {
        val configListJson = objects[listId] ?: return emptyMap()
        val configList = gson.fromJson(configListJson, XcodeProjectStructure.XCConfigurationList::class.java)

        return configList.buildConfigurations.mapNotNull { id ->
            val configJson = objects[id] ?: return@mapNotNull null
            val config = gson.fromJson(configJson, XcodeProjectStructure.XCBuildConfiguration::class.java)
            config.name to id
        }.toMap()
    }
}

private object XcodeProjectStructure {
    data class Pbxproj(
        val objects: Map<String, JsonObject>,
        val rootObject: String,
    )

    data class PbxProject(
        val buildConfigurationList: String,
        val targets: List<String> = emptyList(),
    )

    data class PbxNativeTarget(
        val name: String,
        @SerializedName("productType") val productType: String,
        val buildConfigurationList: String,
    )

    data class XCConfigurationList(
        val buildConfigurations: List<String>,
    )

    data class XCBuildConfiguration(
        val name: String,
        val buildSettings: BuildSettings = BuildSettings(),
    )

    data class BuildSettings(
        @SerializedName("SDKROOT") val sdkRoot: String? = null,
        @SerializedName("SUPPORTED_PLATFORMS") val supportedPlatforms: String? = null,
        @SerializedName("IPHONEOS_DEPLOYMENT_TARGET") val iphoneosDeploymentTarget: String? = null,
        @SerializedName("MACOSX_DEPLOYMENT_TARGET") val macosxDeploymentTarget: String? = null,
        @SerializedName("TVOS_DEPLOYMENT_TARGET") val tvosDeploymentTarget: String? = null,
        @SerializedName("WATCHOS_DEPLOYMENT_TARGET") val watchosDeploymentTarget: String? = null,
    )

    data class XcodeTarget(
        val name: String,
        val productType: String,
        val platforms: Set<String>,
    ) : Serializable
}

private const val unknownSdkRoot = "unknown"

private fun getExpectedSdkRoot(target: KonanTarget) = when (target.family) {
    Family.OSX -> "macosx"
    Family.IOS -> "iphoneos"
    Family.TVOS -> "appletvos"
    Family.WATCHOS -> "watchos"
    else -> unknownSdkRoot
}
