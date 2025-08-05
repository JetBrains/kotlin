/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.*
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeTargetsConfigurationTask.Companion.TASK_NAME
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable
import java.nio.charset.Charset
import javax.inject.Inject

/**
 * Registers the [XcodeTargetsConfigurationTask] to check for mismatches between
 * the Xcode project and the Kotlin Gradle project.
 */
internal val XcodeTargetsConfigurationSetupAction = KotlinProjectSetupAction {
    launch {
        val hasAppleTargets = multiplatformExtension.awaitTargets().any { it is KotlinNativeTarget && it.konanTarget.family.isAppleFamily }
        val iosProjectPath = project.xcodeProjectPath
        if (hasAppleTargets && iosProjectPath != null) {
            locateOrRegisterTask<XcodeTargetsConfigurationTask>(TASK_NAME) { task ->
                val pbxprojContent = project.providers.of(PlistToJsonValueSource::class.java) {
                    it.parameters.pbxprojFile.set(iosProjectPath.resolve("project.pbxproj"))
                }

                task.appleTargets.set(multiplatformExtension.appleTargets.map { it.konanTarget })
                task.iosProjectPath.set(iosProjectPath)
                task.pbxprojContent.set(pbxprojContent)
            }
        }
    }
}

/**
 * A Gradle [ValueSource] that executes the `plutil` command to convert an Xcode
 * project file into JSON format. It catches execution errors and returns an empty
 * string to signal failure gracefully.
 */
internal abstract class PlistToJsonValueSource : ValueSource<String, PlistToJsonValueSource.Params> {

    interface Params : ValueSourceParameters {
        val pbxprojFile: Property<File>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        val file = parameters.pbxprojFile.get().absolutePath
        return try {
            execOperations.exec { spec ->
                spec.executable = "/usr/bin/plutil"
                spec.args("-convert", "json", "-o", "-", file)
                spec.standardOutput = output
            }

            String(output.toByteArray(), Charset.defaultCharset())
        } catch (e: Exception) {
            // Failure (e.g., plutil not found or malformed file), return an empty string.
            ""
        }
    }
}

/**
 * A task that checks for configuration mismatches between Xcode and Kotlin projects.
 * It verifies that for every Apple target defined in Gradle, there is a corresponding
 * application target in the Xcode project.
 */
@DisableCachingByDefault(because = "Xcode targets configuration is not cacheable, it depends on the Xcode project structure and settings.")
internal abstract class XcodeTargetsConfigurationTask : DefaultTask(), UsesKotlinToolingDiagnostics {
    init {
        onlyIf("Task can only run on macOS") { HostManager.hostIsMac }
        onlyIf("Xcode project content is available and not empty") {
            val content = pbxprojContent.orNull
            if (content.isNullOrEmpty()) {
                logger.error(
                    "Failed to read Xcode project file. This may be due to a missing 'plutil' command or a malformed project file. " +
                            "Please ensure Xcode command-line tools are installed."
                )
                false
            } else {
                true
            }
        }
    }

    companion object {
        const val TASK_NAME = "checkXcodeProjectConfiguration"
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:SkipWhenEmpty
    abstract val iosProjectPath: DirectoryProperty

    @get:Internal
    abstract val pbxprojContent: Property<String>

    @get:Internal
    abstract val appleTargets: ListProperty<KonanTarget>

    @TaskAction
    internal fun checkXcodeTargets() {
        val projectContent = pbxprojContent.orNull ?: return
        val xcodeTargets = parseXcodeTargets(projectContent, logger) ?: return

        val xcodeAppTargets = xcodeTargets.filter {
            it.productType == "com.apple.product-type.application" ||
                    it.productType == "com.apple.product-type.application.watchapp2-container"
        }

        if (xcodeAppTargets.isEmpty()) {
            reportDiagnostic(KotlinToolingDiagnostics.NoApplicationTargetFoundDiagnostic(iosProjectPath.getFile()))
            return
        }

        val allXcodeAppPlatforms = xcodeAppTargets.flatMap { it.platforms.keys }.toSet()

        val missingTargets = mutableListOf<Pair<String, String>>()
        for (kotlinTarget in appleTargets.get()) {
            val expectedSdkRoot = getExpectedSdkRoot(kotlinTarget)
            if (expectedSdkRoot != "unknown" && expectedSdkRoot !in allXcodeAppPlatforms) {
                missingTargets.add(kotlinTarget.name to expectedSdkRoot)
            }
        }

        if (missingTargets.isNotEmpty()) {
            reportDiagnostic(
                KotlinToolingDiagnostics.MissingXcodeTargetDiagnostic(missingTargets, iosProjectPath.getFile())
            )
        }
    }

    private fun parseXcodeTargets(
        pbxprojContent: String,
        logger: Logger,
    ): Set<XcodeProjectStructure.XcodeTarget>? {
        val gson = Gson()
        try {
            val pbxproj = gson.fromJson(pbxprojContent, XcodeProjectStructure.Pbxproj::class.java)
            val objects = pbxproj.objects

            val rootProjectJson = objects[pbxproj.rootObject] ?: return emptySet()
            val rootProject = gson.fromJson(rootProjectJson, XcodeProjectStructure.PbxProject::class.java)
            val projectConfigs = getConfigurationMap(gson, objects, rootProject.buildConfigurationList)

            return rootProject.targets.mapNotNull { targetId ->
                parseSingleNativeTarget(gson, targetId, objects, projectConfigs)
            }.toSet()

        } catch (e: Exception) {
            logger.error("Failed to parse the JSON output of 'plutil' for '${iosProjectPath.getFile().path}'. The file might be malformed.\nError: ${e.message ?: "Unknown"}")
            return null
        }
    }

    private fun parseSingleNativeTarget(
        gson: Gson,
        targetId: String,
        objects: Map<String, JsonObject>,
        projectConfigs: Map<String, String>,
    ): XcodeProjectStructure.XcodeTarget? {
        val targetJson = objects[targetId] ?: return null
        if (targetJson.get("isa")?.asString != "PBXNativeTarget") return null

        val nativeTarget = gson.fromJson(targetJson, XcodeProjectStructure.PbxNativeTarget::class.java)
        val targetConfigs = getConfigurationMap(gson, objects, nativeTarget.buildConfigurationList)

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

    private fun getConfigurationMap(gson: Gson, objects: Map<String, JsonObject>, listId: String): Map<String, String> {
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
        val platforms: Map<String, String?>,
    ) : Serializable
}

private fun getExpectedSdkRoot(target: KonanTarget) = when (target.family) {
    Family.OSX -> "macosx"
    Family.IOS -> "iphoneos"
    Family.TVOS -> "appletvos"
    Family.WATCHOS -> "watchos"
    else -> "unknown"
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