/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

private object XcodeEnvironment {
    val buildType: NativeBuildType?
        get() {
            val configuration = System.getenv("CONFIGURATION") ?: return null

            fun String.toNativeBuildType() = when (this.toLowerCase()) {
                "debug" -> NativeBuildType.DEBUG
                "release" -> NativeBuildType.RELEASE
                else -> null
            }

            return configuration.toNativeBuildType()
                ?: System.getenv("KOTLIN_FRAMEWORK_BUILD_TYPE")?.toNativeBuildType()
        }

    val target: KonanTarget?
        get() {
            val sdk = System.getenv("SDK_NAME") ?: return null
            val isForArm = System.getenv("NATIVE_ARCH")?.startsWith("arm") ?: false

            return if (isForArm) when {
                sdk.startsWith("iphoneos") -> KonanTarget.IOS_ARM64
                sdk.startsWith("iphonesimulator") -> KonanTarget.IOS_SIMULATOR_ARM64
                sdk.startsWith("watchos") -> KonanTarget.WATCHOS_ARM64
                sdk.startsWith("watchsimulator") -> KonanTarget.WATCHOS_SIMULATOR_ARM64
                sdk.startsWith("appletvos") -> KonanTarget.TVOS_ARM64
                sdk.startsWith("appletvsimulator") -> KonanTarget.TVOS_SIMULATOR_ARM64
                sdk.startsWith("macosx") -> KonanTarget.MACOS_ARM64
                else -> null
            } else when {
                sdk.startsWith("iphonesimulator") -> KonanTarget.IOS_X64
                sdk.startsWith("watchsimulator") -> KonanTarget.WATCHOS_X64
                sdk.startsWith("appletvsimulator") -> KonanTarget.TVOS_X64
                sdk.startsWith("macosx") -> KonanTarget.MACOS_X64
                else -> null
            }
        }

    val frameworkSearchDir: File?
        get() {
            val configuration = System.getenv("CONFIGURATION") ?: return null
            val sdk = System.getenv("SDK_NAME") ?: return null
            return File(configuration, sdk)
        }

    val embeddedFrameworksDir: File?
        get() {
            val xcodeTargetBuildDir = System.getenv("TARGET_BUILD_DIR") ?: return null
            val xcodeFrameworksFolderPath = System.getenv("FRAMEWORKS_FOLDER_PATH") ?: return null
            return File(xcodeTargetBuildDir, xcodeFrameworksFolderPath)
        }

    val sign: String? get() = System.getenv("EXPANDED_CODE_SIGN_IDENTITY")
}

private fun Project.registerAssembleAppleFrameworkTask(framework: Framework): TaskProvider<Copy>? {
    if (!framework.konanTarget.family.isAppleFamily || !framework.konanTarget.enabledOnCurrentHost) return null

    val frameworkBuildType = framework.buildType
    val frameworkTarget = framework.target
    val frameworkTaskName = lowerCamelCaseName(
        "assemble",
        framework.namePrefix,
        frameworkBuildType.getName(),
        "AppleFrameworkForXcode",
        frameworkTarget.name
    )

    val envBuildType = XcodeEnvironment.buildType
    val envTarget = XcodeEnvironment.target
    val envFrameworkSearchDir = XcodeEnvironment.frameworkSearchDir

    if (envBuildType == null || envTarget == null || envFrameworkSearchDir == null) {
        val envConfiguration = System.getenv("CONFIGURATION")
        if (envTarget != null && envConfiguration != null) {
            logger.warn(
                "Unable to detect Kotlin framework build type for CONFIGURATION=$envConfiguration automatically. " +
                        "Specify 'KOTLIN_FRAMEWORK_BUILD_TYPE' to 'debug' or 'release'"
            )
        } else {
            logger.debug(
                "Not registering $frameworkTaskName, since not called from Xcode " +
                        "('SDK_NAME' and 'CONFIGURATION' not provided)"
            )
        }
        return null
    }

    if (frameworkBuildType != envBuildType || frameworkTarget.konanTarget != envTarget) return null

    return registerTask(frameworkTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
        task.dependsOn(framework.linkTaskName)
        task.from(framework.outputDirectory)
        task.into(appleFrameworkDir(envFrameworkSearchDir))
    }
}

internal fun Project.registerEmbedAndSignAppleFrameworkTask(framework: Framework) {
    val assembleTask = registerAssembleAppleFrameworkTask(framework) ?: return

    val envBuildType = XcodeEnvironment.buildType
    val envTarget = XcodeEnvironment.target
    val envEmbeddedFrameworksDir = XcodeEnvironment.embeddedFrameworksDir
    val envFrameworkSearchDir = XcodeEnvironment.frameworkSearchDir
    val envSign = XcodeEnvironment.sign

    val frameworkTaskName = lowerCamelCaseName("embedAndSign", framework.namePrefix, "AppleFrameworkForXcode")

    if (envBuildType == null || envTarget == null || envEmbeddedFrameworksDir == null || envFrameworkSearchDir == null) {
        logger.debug(
            "Not registering $frameworkTaskName, since not called from Xcode " +
                    "('SDK_NAME', 'CONFIGURATION', 'TARGET_BUILD_DIR' and 'FRAMEWORKS_FOLDER_PATH' not provided)"
        )
        return
    }

    if (framework.buildType != envBuildType || framework.konanTarget != envTarget) return

    registerTask<Copy>(frameworkTaskName) { task ->
        task.group = "build"
        task.description = "Embed and sign ${framework.namePrefix} framework as requested by Xcode's environment variables"

        task.dependsOn(assembleTask)
        task.inputs.apply {
            property("type", envBuildType)
            property("target", envTarget)
            property("embeddedFrameworksDir", envEmbeddedFrameworksDir)
            property("sign", envSign)
        }

        task.from(appleFrameworkDir(envFrameworkSearchDir)) { spec ->
            spec.include(framework.outputFile.name + "/**")
        }

        task.into(envEmbeddedFrameworksDir)

        if (envSign != null) {
            task.doLast {
                val binary = envEmbeddedFrameworksDir
                    .resolve(framework.outputFile.name)
                    .resolve(framework.outputFile.nameWithoutExtension)
                exec {
                    it.commandLine("codesign", "--force", "--sign", envSign, "--", binary)
                }
            }
        }
    }
}

private val Framework.namePrefix: String
    get() = KotlinNativeBinaryContainer.extractPrefixFromBinaryName(
        name,
        buildType,
        outputKind.taskNameClassifier
    )

private fun Project.appleFrameworkDir(frameworkSearchDir: File) =
    buildDir.resolve("xcode-frameworks").resolve(frameworkSearchDir)