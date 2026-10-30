/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

internal class XcodeEnvironment(private val project: Project) {
    companion object {
        const val XCODE_ENVIRONMENT_OVERRIDE_KEY = "kotlin.envOverride"
    }

    val buildType: NativeBuildType?
        get() {
            val configuration = readEnvVariable("CONFIGURATION") ?: return null

            fun String.toNativeBuildType() = when (this.toLowerCaseAsciiOnly()) {
                "debug" -> NativeBuildType.DEBUG
                "release" -> NativeBuildType.RELEASE
                else -> null
            }

            return configuration.toNativeBuildType()
                ?: readEnvVariable("KOTLIN_FRAMEWORK_BUILD_TYPE")?.toNativeBuildType()
        }

    val targets: List<KonanTarget>
        get() {
            val sdk = readEnvVariable("SDK_NAME") ?: return emptyList()
            val archs = readEnvVariable("ARCHS")?.split(" ") ?: return emptyList()
            val isCatalyst = readEnvVariable("IS_MACCATALYST") == "YES"
            return AppleSdk.defineNativeTargets(sdk, archs, isCatalyst)
        }

    val frameworkSearchDir: File?
        get() {
            val configuration = readEnvVariable("CONFIGURATION") ?: return null
            val sdk = readEnvVariable("SDK_NAME") ?: return null
            return File(configuration, sdk)
        }

    val builtProductsDir: File?
        get() = readEnvVariable("BUILT_PRODUCTS_DIR")?.let(::File)

    val buildDir: File?
        get() = readEnvVariable("BUILD_DIR")?.let(::File)

    val effectivePlatformName: String?
        get() = readEnvVariable("EFFECTIVE_PLATFORM_NAME")

    /**
     * Where Xcode's indexer expects `BUILT_PRODUCTS_DIR`. Since Xcode 13 indexing always uses the Debug configuration,
     * so a framework built for another configuration is invisible to code completion unless it is reachable from here
     * (KT-76278).
     *
     * Xcode defines `BUILT_PRODUCTS_DIR` as `$(BUILD_DIR)/$(CONFIGURATION)$(EFFECTIVE_PLATFORM_NAME)`; the editor keeps
     * a second copy under `<DerivedData>/Index.noindex/Build/Products`. [builtProductsDir] itself is excluded, the regular
     * symbolic link already covers it.
     */
    val indexBuiltProductsDirs: List<File>
        get() {
            // archives build into ArchiveIntermediates, which no indexer reads
            if (action == Action.install) return emptyList()
            val buildDir = buildDir ?: return emptyList()
            val configuration = readEnvVariable("CONFIGURATION") ?: return emptyList()
            val effectivePlatformName = effectivePlatformName ?: return emptyList()
            val builtProductsDir = builtProductsDir ?: return emptyList()

            // a project overriding CONFIGURATION_BUILD_DIR breaks the formula, and with it our guess where the indexer looks
            if (!builtProductsDir.isSameDirectory(buildDir.resolve("$configuration$effectivePlatformName"))) return emptyList()

            val debugProducts = buildDir.resolve("Debug$effectivePlatformName")
            val candidates = mutableListOf(debugProducts)
            // <DerivedData>/Build/Products -> <DerivedData>/Index.noindex/Build/Products
            val derivedData = buildDir.takeIf { it.name == "Products" }?.parentFile?.takeIf { it.name == "Build" }?.parentFile
            if (derivedData != null) {
                candidates += derivedData.resolve("Index.noindex/Build/Products").resolve(debugProducts.name)
            }
            return candidates.filterNot { it.isSameDirectory(builtProductsDir) }
        }

    // the default Apple file system is case-insensitive, so `debug-iphoneos` and `Debug-iphoneos` are one directory
    private fun File.isSameDirectory(other: File): Boolean = normalize().path.equals(other.normalize().path, ignoreCase = true)

    val embeddedFrameworksDir: File?
        get() {
            val xcodeTargetBuildDir = readEnvVariable("TARGET_BUILD_DIR") ?: return null
            val xcodeFrameworksFolderPath = readEnvVariable("FRAMEWORKS_FOLDER_PATH") ?: return null
            return File(xcodeTargetBuildDir, xcodeFrameworksFolderPath).absoluteFile
        }

    val sign: String?
        get() = readEnvVariable("EXPANDED_CODE_SIGN_IDENTITY")

    /**
     * Name of the build setting holding the deployment target of the Xcode target being built,
     * for example `IPHONEOS_DEPLOYMENT_TARGET`. Xcode picks it based on the platform.
     */
    val deploymentTargetSettingName: String?
        get() = readEnvVariable("DEPLOYMENT_TARGET_SETTING_NAME")

    /**
     * Value of the [deploymentTargetSettingName] build setting, for example `18.0`.
     */
    val deploymentTarget: String?
        get() = deploymentTargetSettingName?.let { readEnvVariable(it) }

    val userScriptSandboxingEnabled: Boolean
        get() = readEnvVariable("ENABLE_USER_SCRIPT_SANDBOXING") == "YES"

    enum class Action {
        // xcodebuild archive
        install,
        // xcodebuild build/test/etc
        other,
    }

    val action: Action?
        get() = readEnvVariable("ACTION")?.let {
            when (it) {
                "install" -> Action.install
                else -> Action.other
            }
        }

    val dwarfDsymFolderPath: String?
        get() = readEnvVariable("DWARF_DSYM_FOLDER_PATH")

    private fun readEnvVariable(name: String): String? {
        return project.extensions.extraProperties.getOrNull("$XCODE_ENVIRONMENT_OVERRIDE_KEY.$name") as? String ?: System.getenv(name)
    }

    override fun toString() = """
        XcodeEnvironment:
          buildType=$buildType
          targets=$targets
          frameworkSearchDir=$frameworkSearchDir
          builtProductDir=$builtProductsDir
          indexBuiltProductsDirs=$indexBuiltProductsDirs
          embeddedFrameworksDir=$embeddedFrameworksDir
          sign=$sign
          userScriptSandboxingEnabled=$userScriptSandboxingEnabled
          deploymentTarget=$deploymentTargetSettingName=$deploymentTarget
    """.trimIndent()
}
