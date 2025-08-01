/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.Serializable
import javax.inject.Inject

/**
 * Registers two tasks:
 * 1. A producer task that converts the `.pbxproj` file to JSON.
 * 2. A consumer task that reads the JSON and performs the configuration check.
 * This ensures the expensive `plutil` command is only run when the Xcode project changes,
 * while the cheap validation task runs whenever the JSON changes, consistently re-issuing warnings.
 */
internal val CheckXcodeTargetsConfigurationSetupAction = KotlinProjectSetupAction {
    launch {
        if (!shouldSetupXcodeConfiguration()) {
            return@launch
        }

        val appleTargets = getAppleTargetsWithFrameworkBinaries()
        val projectPath = project.xcodeProjectPath ?: return@launch

        val convertTask = registerConvertPbxprojToJsonTask(projectPath)
        registerCheckXcodeTargetsConfigurationTask(appleTargets, projectPath, convertTask)
    }
}

private suspend fun Project.shouldSetupXcodeConfiguration(): Boolean {
    val targets = multiplatformExtension
        .awaitTargets()
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.konanTarget.family.isAppleFamily }

    if (targets.isEmpty()) return false

    val hasBinaries = targets.flatMap { it.binaries }.filterIsInstance<Framework>().isNotEmpty()
    if (!hasBinaries) return false

    return project.xcodeProjectPath != null
}

private suspend fun Project.getAppleTargetsWithFrameworkBinaries(): List<KonanTarget> {
    return multiplatformExtension
        .awaitTargets()
        .filterIsInstance<KotlinNativeTarget>()
        .filter { target ->
            target.konanTarget.family.isAppleFamily &&
                    target.binaries.filterIsInstance<Framework>().isNotEmpty()
        }
        .map { it.konanTarget }
}

private fun Project.registerConvertPbxprojToJsonTask(
    projectPath: File,
): TaskProvider<ConvertPbxprojToJsonTask> {
    return locateOrRegisterTask(ConvertPbxprojToJsonTask.TASK_NAME) {
        it.group = "xcode"
        it.description = "Converts .pbxproj file to JSON for inspection"
        it.pbxprojFile.set(projectPath.resolve("project.pbxproj"))
        it.jsonFile.set(layout.buildDirectory.file("xcode-check/project.json"))
    }
}

private fun Project.registerCheckXcodeTargetsConfigurationTask(
    targets: List<KonanTarget>,
    projectPath: File,
    convertTask: TaskProvider<ConvertPbxprojToJsonTask>,
) {
    locateOrRegisterTask<CheckXcodeTargetsConfigurationTask>(
        CheckXcodeTargetsConfigurationTask.TASK_NAME,
        invokeWhenRegistered = {
            @OptIn(Idea222Api::class)
            ideaImportDependsOn(this)
        },
        configureTask = {
            group = "xcode"
            description = "Checks for configuration mismatches between Xcode and Kotlin Gradle project"

            appleTargets.set(targets)
            xcodeProjectPath.set(projectPath)
            pbxprojJson.set(convertTask.flatMap { it.jsonFile })
        }
    )
}

/**
 * Producer Task: Converts the `.pbxproj` file to JSON using `plutil`.
 * This task is fully cacheable.
 */
@CacheableTask
internal abstract class ConvertPbxprojToJsonTask : DefaultTask() {
    init {
        onlyIf("Task can only run on macOS") { HostManager.hostIsMac }
    }

    companion object {
        const val TASK_NAME = "convertPbxprojToJson"
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val pbxprojFile: RegularFileProperty

    @get:OutputFile
    abstract val jsonFile: RegularFileProperty

    @TaskAction
    fun run() {
        try {
            execOperations.exec { spec ->
                spec.commandLine(
                    "plutil",
                    "-convert", "json",
                    "-o", jsonFile.get().asFile.absolutePath,
                    pbxprojFile.get().asFile.absolutePath
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to execute 'plutil' on '${pbxprojFile.get().asFile.path}'. The file might be malformed or 'plutil' is not in PATH.")
            jsonFile.get().asFile.writeText("{}") // Write empty JSON on failure
        }
    }
}


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

        val xcodeAppTargets = xcodeTargets.filter {
            it.productType == "com.apple.product-type.application" ||
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

        } catch (e: Exception) {
            logger.error(
                "Failed to parse the JSON file for '${xcodeProjectPath.getFile().path}'. The file might be malformed.\nError: ${e.message ?: "Unknown"}"
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

private val Project.xcodeProjectPath: File?
    get() {
        // A heuristic to find the `.xcodeproj` directory.
        // It's not guaranteed to work for all project structures.
        val commonPath = "iosApp/iosApp.xcodeproj"
        val iosProject = layout.projectDirectory.asFile.resolve(commonPath)
        if (iosProject.exists()) return iosProject

        val rootProjectIos = rootDir.resolve(commonPath)
        if (rootProjectIos.exists()) return rootProjectIos

        return null
    }
