/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.konan.target.Family
import java.io.File
import java.io.Serializable as JavaSerializable

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
        val platforms: Map<String, String?>,
    ) : JavaSerializable
}

/**
 * This checker verifies that the Xcode project is correctly set up to consume frameworks
 * from a Kotlin Multiplatform project. It uses the macOS `plutil` command to safely convert
 * the project.pbxproj file to JSON, and then parses that JSON using `Gson`
 * to inspect the build settings.
 */
internal object XcodeProjectSetupChecker : KotlinGradleProjectChecker {
    private val gson by lazy { Gson() }

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val multiplatformExtension = multiplatformExtension ?: return
        val iosProjectPath = project.xcodeProjectPath ?: return
        val pbxprojFile = iosProjectPath.resolve("project.pbxproj").takeIf { it.exists() } ?: return

        AfterFinaliseDsl.await()

        val appleTargets = multiplatformExtension.appleTargets.takeIf { it.isNotEmpty() } ?: return

        val xcodeTargets = parseXcodeTargets(pbxprojFile, project.logger) ?: return

        val xcodeAppTargets = xcodeTargets.filter {
            it.productType == "com.apple.product-type.application" ||
                    it.productType == "com.apple.product-type.application.watchapp2-container"
        }

        if (xcodeAppTargets.isEmpty()) {
            collector.report(project, KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic(iosProjectPath))
            return
        }

        val allXcodeAppPlatforms = xcodeAppTargets.flatMap { it.platforms.keys }.toSet()

        val missingTargets = mutableListOf<Pair<String, String>>()
        for (kotlinTarget in appleTargets) {
            val expectedSdkRoot = getExpectedSdkRoot(kotlinTarget)
            if (expectedSdkRoot != "unknown" && expectedSdkRoot !in allXcodeAppPlatforms) {
                missingTargets.add(kotlinTarget.name to expectedSdkRoot)
            }
        }

        if (missingTargets.isNotEmpty()) {
            collector.report(
                project,
                KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic(missingTargets, iosProjectPath)
            )
        }
    }

    private fun parseXcodeTargets(
        pbxprojFile: File,
        logger: Logger,
    ): Set<XcodeProjectStructure.XcodeTarget>? {
        val jsonOutput = try {
            runPlutil(pbxprojFile, logger)
        } catch (e: IllegalStateException) {
            logger.error(
                "Failed to execute 'plutil' on '${pbxprojFile.path}'. This utility is required to parse Xcode project files. " +
                        "Please ensure Xcode command-line tools are installed and accessible.\nError: ${e.message ?: "Unknown"}"
            )
            return null
        }

        try {
            val pbxproj = gson.fromJson(jsonOutput, XcodeProjectStructure.Pbxproj::class.java)
            val objects = pbxproj.objects

            val rootProjectJson = objects[pbxproj.rootObject] ?: return emptySet()
            val rootProject = gson.fromJson(rootProjectJson, XcodeProjectStructure.PbxProject::class.java)
            val projectConfigs = getConfigurationMap(objects, rootProject.buildConfigurationList)

            return rootProject.targets.mapNotNull { targetId ->
                parseSingleNativeTarget(targetId, objects, projectConfigs)
            }.toSet()

        } catch (e: Exception) {
            logger.error("Failed to parse the JSON output of 'plutil' for '${pbxprojFile.path}'. The file might be malformed.\nError: ${e.message ?: "Unknown"}")
            return null
        }
    }

    private fun parseSingleNativeTarget(
        targetId: String,
        objects: Map<String, JsonObject>,
        projectConfigs: Map<String, String>,
    ): XcodeProjectStructure.XcodeTarget? {
        val targetJson = objects[targetId] ?: return null
        if (targetJson.get("isa")?.asString != "PBXNativeTarget") return null

        val nativeTarget = gson.fromJson(targetJson, XcodeProjectStructure.PbxNativeTarget::class.java)
        val targetConfigs = getConfigurationMap(objects, nativeTarget.buildConfigurationList)

        val platforms = mutableMapOf<String, String?>()

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
                        val deploymentTarget = getDeploymentTarget(sdk, targetSettings, projectSettings)
                        platforms[sdk] = deploymentTarget
                    }
                }
            } else {
                val sdkRoot = targetSettings.sdkRoot ?: projectSettings.sdkRoot
                if (sdkRoot != null) {
                    val deploymentTarget = getDeploymentTarget(sdkRoot, targetSettings, projectSettings)
                    platforms[sdkRoot] = deploymentTarget
                }
            }
        }

        return if (platforms.isNotEmpty()) {
            XcodeProjectStructure.XcodeTarget(nativeTarget.name, nativeTarget.productType, platforms)
        } else {
            null
        }
    }

    private fun getConfigurationMap(objects: Map<String, JsonObject>, listId: String): Map<String, String> {
        val configListJson = objects[listId] ?: return emptyMap()
        val configList = gson.fromJson(configListJson, XcodeProjectStructure.XCConfigurationList::class.java)

        return configList.buildConfigurations.mapNotNull { id ->
            val configJson = objects[id] ?: return@mapNotNull null
            val config = gson.fromJson(configJson, XcodeProjectStructure.XCBuildConfiguration::class.java)
            config.name to id
        }.toMap()
    }

    private fun getDeploymentTarget(
        sdk: String,
        target: XcodeProjectStructure.BuildSettings,
        project: XcodeProjectStructure.BuildSettings,
    ): String? {
        return when (sdk) {
            "iphoneos", "iphonesimulator" -> target.iphoneosDeploymentTarget ?: project.iphoneosDeploymentTarget
            "watchos", "watchsimulator" -> target.watchosDeploymentTarget ?: project.watchosDeploymentTarget
            "appletvos", "appletvsimulator" -> target.tvosDeploymentTarget ?: project.tvosDeploymentTarget
            "macosx" -> target.macosxDeploymentTarget ?: project.macosxDeploymentTarget
            else -> null
        }
    }

    private fun runPlutil(pbxprojFile: File, logger: Logger) = runCommand(
        listOf("plutil", "-convert", "json", "-o", "-", pbxprojFile.absolutePath),
        logger,
        errorHandler = { result ->
            "Failed to execute 'plutil' on '${pbxprojFile.path}'. Error: ${result.stdErr}"
        }
    )
}

private fun getExpectedSdkRoot(target: KotlinNativeTarget): String {
    val isSimulator = target.konanTarget.name.contains("simulator", ignoreCase = true)
    return when (target.konanTarget.family) {
        Family.OSX -> "macosx"
        Family.IOS -> if (isSimulator) "iphonesimulator" else "iphoneos"
        Family.TVOS -> if (isSimulator) "appletvsimulator" else "appletvos"
        Family.WATCHOS -> if (isSimulator) "watchsimulator" else "watchos"
        else -> "unknown"
    }
}

private val Project.xcodeProjectPath: File?
    get() {
        // A heuristic to find the `.xcodeproj` directory.
        // It's not guaranteed to work for all project structures.
        val commonPath = "iosApp/iosApp.xcodeproj"
        val iosProject = layout.projectDirectory.asFile.resolve(commonPath)
        if (iosProject.exists()) return iosProject

        val rootProjectIos = layout.projectDirectory.asFile.parentFile?.resolve(commonPath)
        if (rootProjectIos?.exists() == true) return rootProjectIos

        return null
    }

private val KotlinMultiplatformExtension.appleTargets: DomainObjectCollection<KotlinNativeTarget>
    get() = targets
        .withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget.family.isAppleFamily }
