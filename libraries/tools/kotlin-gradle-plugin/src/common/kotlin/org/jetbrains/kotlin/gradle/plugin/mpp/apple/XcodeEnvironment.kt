/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

internal interface XcodeEnvironment {
    val buildType: NativeBuildType?
    val targets: List<KonanTarget>

    val frameworkSearchDir: File?
    val builtProductsDir: File?
    val embeddedFrameworksDir: File?

    val sign: String?
    val userScriptSandboxingEnabled: Boolean
}

internal object XcodeEnvironmentContainer {
    const val XCODE_ENVIRONMENT_KEY = "XCODE_ENVIRONMENT_KEY"

    private var _environment: XcodeEnvironment? = null

    val environment: XcodeEnvironment
        get() = _environment ?: throw IllegalStateException("Xcode environment has not been set")

    fun setupEnvironment(project: Project) {
        _environment = project.extraProperties.getOrNull(XCODE_ENVIRONMENT_KEY) as? XcodeEnvironment ?: AppleXcodeEnvironment()
    }
}

internal fun XcodeEnvironment.printDebugInfo() = """
        XcodeEnvironment:
          buildType=$buildType
          targets=$targets
          frameworkSearchDir=$frameworkSearchDir
          builtProductDir=$builtProductsDir
          embeddedFrameworksDir=$embeddedFrameworksDir
          sign=$sign
          userScriptSandboxingEnabled=$userScriptSandboxingEnabled
    """.trimIndent()

private class AppleXcodeEnvironment : XcodeEnvironment {
    override val buildType: NativeBuildType?
        get() {
            val configuration = System.getenv("CONFIGURATION") ?: return null

            fun String.toNativeBuildType() = when (this.toLowerCaseAsciiOnly()) {
                "debug" -> NativeBuildType.DEBUG
                "release" -> NativeBuildType.RELEASE
                else -> null
            }

            return configuration.toNativeBuildType()
                ?: System.getenv("KOTLIN_FRAMEWORK_BUILD_TYPE")?.toNativeBuildType()
        }

    override val targets: List<KonanTarget>
        get() {
            val sdk = System.getenv("SDK_NAME") ?: return emptyList()
            val archs = System.getenv("ARCHS")?.split(" ") ?: return emptyList()
            return AppleSdk.defineNativeTargets(sdk, archs)
        }

    override val frameworkSearchDir: File?
        get() {
            val configuration = System.getenv("CONFIGURATION") ?: return null
            val sdk = System.getenv("SDK_NAME") ?: return null
            return File(configuration, sdk)
        }

    override val builtProductsDir: File?
        get() = System.getenv("BUILT_PRODUCTS_DIR")?.let(::File)

    override val embeddedFrameworksDir: File?
        get() {
            val xcodeTargetBuildDir = System.getenv("TARGET_BUILD_DIR") ?: return null
            val xcodeFrameworksFolderPath = System.getenv("FRAMEWORKS_FOLDER_PATH") ?: return null
            return File(xcodeTargetBuildDir, xcodeFrameworksFolderPath).absoluteFile
        }

    override val sign: String?
        get() = System.getenv("EXPANDED_CODE_SIGN_IDENTITY")

    override val userScriptSandboxingEnabled: Boolean
        get() = System.getenv("ENABLE_USER_SCRIPT_SANDBOXING") == "YES"

    override fun toString() = printDebugInfo()
}