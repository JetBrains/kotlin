/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable as JsonSerializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj.XcprojNativeTarget.Companion.NATIVE_TARGET_KIND
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
 * 1. A producer task that converts `.pbxproj` to JSON, or copies an existing `.xcproj` JSON file.
 * 2. A consumer task that reads the JSON and performs the configuration check.
 * This ensures the expensive `plutil` command is only run when the Xcode project changes,
 * while the cheap validation task runs whenever the JSON changes, consistently re-issuing warnings.
 */
internal val CheckXcodeTargetsConfigurationSetupAction = KotlinProjectSetupCoroutine {
    // 1. Check if there are any apple targets with frameworks. If not, the check is not needed.
    if (!shouldSetupXcodeConfiguration()) {
        return@KotlinProjectSetupCoroutine
    }

    // 2. Check for the Xcode project path. If it's not found, log an informational message.
    val projectPath = project.xcodeProjectPath
    if (projectPath == null) {
        val searchedPaths = project.xcodeProjectSearchedPaths
        logger.info(
            "Kotlin Xcode project checker: .xcodeproj directory not found. Searched in:\n" +
                    searchedPaths.joinToString("\n") { " - ${it.path}" } +
                    "\nSkipping task registration."
        )
        return@KotlinProjectSetupCoroutine
    }

    // 3. If everything is in place, register the tasks.
    val appleTargets = getAppleTargetsWithFrameworkBinaries()
    val convertTask = registerConvertPbxprojToJsonTask(projectPath)
    registerCheckXcodeTargetsConfigurationTask(appleTargets, projectPath, convertTask)
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
): TaskProvider<ConvertXcodeProjectFileToJsonTask> {
    return locateOrRegisterTask(ConvertXcodeProjectFileToJsonTask.TASK_NAME) {
        it.group = "xcode"
        it.description = "Prepares Xcode project JSON for inspection"
        it.xcodeProjectFile.set(projectPath.xcodeProjectFile())
        it.jsonFile.set(layout.buildDirectory.file("xcode-check/project.json"))
    }
}

private fun Project.registerCheckXcodeTargetsConfigurationTask(
    targets: List<KonanTarget>,
    projectPath: File,
    convertTask: TaskProvider<ConvertXcodeProjectFileToJsonTask>,
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
 * Producer Task: Converts `.pbxproj` to JSON using `plutil`, or copies `.xcproj` without conversion.
 * This task is fully cacheable.
 */
@CacheableTask
internal abstract class ConvertXcodeProjectFileToJsonTask : DefaultTask() {
    init {
        onlyIf("Task can only run on macOS") { HostManager.hostIsMac }
    }

    companion object {
        const val TASK_NAME = "convertPbxprojToJson"
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val xcodeProjectFile: RegularFileProperty

    @get:OutputFile
    abstract val jsonFile: RegularFileProperty

    @TaskAction
    fun run() {
        val projectFile = xcodeProjectFile.get().asFile
        if (projectFile.isXcproj()) {
            projectFile.copyTo(jsonFile.get().asFile, overwrite = true)
        } else {
            // pbxproj
            try {
                execOperations.exec { spec ->
                    spec.commandLine(
                        "plutil",
                        "-convert", "json",
                        "-o", jsonFile.get().asFile.absolutePath,
                        projectFile.absolutePath
                    )
                }
            } catch (exception: Exception) {
                logger.error(
                    "Failed to execute 'plutil' on '${projectFile.path}'. The file might be malformed or 'plutil' is not in PATH.",
                    exception
                )
                jsonFile.get().asFile.writeText("{}") // Write empty JSON on failure
            }
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
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val xcodeProjectPath: DirectoryProperty

    @get:InputFile
    @get:NormalizeLineEndings
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

        val xcodeTargets = parseXcodeTargets(pbxprojContent, logger) ?: return

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
        logger: Logger,
    ): Set<XcodeProjectStructure.XcodeTarget>? {
        try {
            if (xcodeProjectPath.getFile().xcodeProjectFile().isXcproj()) {
                val project = deserializeXcprojProject(pbxprojContent)
                return project.nativeTargets.mapNotNull { target ->
                    val platforms = platforms(target)
                    if (platforms.isEmpty()) null else XcodeProjectStructure.XcodeTarget(
                        target.name, target.resolvedProductType, platforms,
                    )
                }.toSet()
            } else {
                val pbxproj = pbxprojJsonReader.decodeFromString(XcodeProjectStructure.Pbxproj.serializer(), pbxprojContent)
                val objects = pbxproj.objects

                val rootProjectJson = objects[pbxproj.rootObject] ?: return emptySet()
                val rootProject = rootProjectJson.decode(XcodeProjectStructure.PbxProject.serializer())
                val projectConfigs = getConfigurationMap(objects, rootProject.buildConfigurationList)

                return rootProject.targets.mapNotNull { targetId ->
                    parseSingleNativeTarget(targetId, objects, projectConfigs)
                }.toSet()
            }
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
    ): XcodeProjectStructure.XcodeTarget? {
        val targetJson = objects[targetId] ?: return null
        if ((targetJson["isa"] as? JsonPrimitive)?.contentOrNull != "PBXNativeTarget") return null

        val nativeTarget = targetJson.decode(XcodeProjectStructure.PbxNativeTarget.serializer())
        val targetConfigs = getConfigurationMap(objects, nativeTarget.buildConfigurationList)

        val platforms = mutableSetOf<String>()

        for ((configName, configId) in targetConfigs) {
            val targetConfig = objects.getValue(configId).decode(XcodeProjectStructure.XCBuildConfiguration.serializer())
            val projectConfigId = projectConfigs[configName]
            val projectConfig =
                projectConfigId?.let { objects[it] }?.decode(XcodeProjectStructure.XCBuildConfiguration.serializer())

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
    ): Map<String, String> {
        val configListJson = objects[listId] ?: return emptyMap()
        val configList = configListJson.decode(XcodeProjectStructure.XCConfigurationList.serializer())

        return configList.buildConfigurations.mapNotNull { id ->
            val configJson = objects[id] ?: return@mapNotNull null
            val config = configJson.decode(XcodeProjectStructure.XCBuildConfiguration.serializer())
            config.name to id
        }.toMap()
    }
}

/** `plutil` output, read as leniently as before; keys the checker doesn't use are ignored. */
private val pbxprojJsonReader = Json(KgpJson.default) { isLenient = true }

private fun <T> JsonObject.decode(deserializer: DeserializationStrategy<T>): T =
    pbxprojJsonReader.decodeFromJsonElement(deserializer, this)

private object XcodeProjectStructure {
    @JsonSerializable
    data class Pbxproj(
        val objects: Map<String, JsonObject>,
        val rootObject: String,
    )

    @JsonSerializable
    data class PbxProject(
        val buildConfigurationList: String,
        val targets: List<String> = emptyList(),
    )

    @JsonSerializable
    data class PbxNativeTarget(
        val name: String,
        val productType: String,
        val buildConfigurationList: String,
    )

    @JsonSerializable
    data class XCConfigurationList(
        val buildConfigurations: List<String>,
    )

    @JsonSerializable
    data class XCBuildConfiguration(
        val name: String,
        val buildSettings: BuildSettings = BuildSettings(),
    )

    @JsonSerializable
    data class BuildSettings(
        @SerialName("SDKROOT") val sdkRoot: String? = null,
        @SerialName("SUPPORTED_PLATFORMS") val supportedPlatforms: String? = null,
        @SerialName("IPHONEOS_DEPLOYMENT_TARGET") val iphoneosDeploymentTarget: String? = null,
        @SerialName("MACOSX_DEPLOYMENT_TARGET") val macosxDeploymentTarget: String? = null,
        @SerialName("TVOS_DEPLOYMENT_TARGET") val tvosDeploymentTarget: String? = null,
        @SerialName("WATCHOS_DEPLOYMENT_TARGET") val watchosDeploymentTarget: String? = null,
    )

    data class XcodeTarget(
        val name: String,
        val productType: String?,
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

private val Project.xcodeProjectSearchedPaths: List<File>
    get() {
        val commonPath = "iosApp/iosApp.xcodeproj"
        return listOf(
            layout.projectDirectory.asFile.resolve(commonPath),
            rootDir.resolve(commonPath)
        )
    }

private val Project.xcodeProjectPath: File?
    get() = xcodeProjectSearchedPaths.firstOrNull { it.exists() }
