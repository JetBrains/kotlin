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
            return AppleSdk.defineNativeTargets(sdk, archs)
        }

    val frameworkSearchDir: File?
        get() {
            val configuration = readEnvVariable("CONFIGURATION") ?: return null
            val sdk = readEnvVariable("SDK_NAME") ?: return null
            return File(configuration, sdk)
        }

    val builtProductsDir: File?
        get() = readEnvVariable("BUILT_PRODUCTS_DIR")?.let(::File)

    val embeddedFrameworksDir: File?
        get() {
            val xcodeTargetBuildDir = readEnvVariable("TARGET_BUILD_DIR") ?: return null
            val xcodeFrameworksFolderPath = readEnvVariable("FRAMEWORKS_FOLDER_PATH") ?: return null
            return File(xcodeTargetBuildDir, xcodeFrameworksFolderPath).absoluteFile
        }

    val sign: String?
        get() = readEnvVariable("EXPANDED_CODE_SIGN_IDENTITY")

    val userScriptSandboxingEnabled: Boolean
        get() = readEnvVariable("ENABLE_USER_SCRIPT_SANDBOXING") == "YES"

    private fun readEnvVariable(name: String): String? {
        return project.extensions.extraProperties.getOrNull("$XCODE_ENVIRONMENT_OVERRIDE_KEY.$name") as? String ?: System.getenv(name)
    }

    override fun toString() = """
        XcodeEnvironment:
          buildType=$buildType
          targets=$targets
          frameworkSearchDir=$frameworkSearchDir
          builtProductDir=$builtProductsDir
          embeddedFrameworksDir=$embeddedFrameworksDir
          sign=$sign
          userScriptSandboxingEnabled=$userScriptSandboxingEnabled
    """.trimIndent()
}