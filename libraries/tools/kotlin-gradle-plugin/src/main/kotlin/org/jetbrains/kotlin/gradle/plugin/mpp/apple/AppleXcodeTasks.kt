/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
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


    val targets: List<KonanTarget>
        get() {
            val sdk = System.getenv("SDK_NAME") ?: return emptyList()
            val archs = System.getenv("ARCHS")?.split(" ") ?: return emptyList()
            return AppleSdk.defineNativeTargets(sdk, archs)
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

private fun Project.registerAssembleAppleFrameworkTask(framework: Framework): TaskProvider<out Task>? {
    if (!framework.konanTarget.family.isAppleFamily || !framework.konanTarget.enabledOnCurrentHost) return null

    val envTargets = XcodeEnvironment.targets
    val needFatFramework = envTargets.size > 1

    val frameworkBuildType = framework.buildType
    val frameworkTarget = framework.target
    val frameworkTaskName = lowerCamelCaseName(
        "assemble",
        framework.namePrefix,
        frameworkBuildType.getName(),
        "AppleFrameworkForXcode",
        if (!needFatFramework) frameworkTarget.name else null
    )

    val envBuildType = XcodeEnvironment.buildType
    val envFrameworkSearchDir = XcodeEnvironment.frameworkSearchDir

    if (envBuildType == null || envTargets.isEmpty() || envFrameworkSearchDir == null) {
        val envConfiguration = System.getenv("CONFIGURATION")
        if (envTargets.isNotEmpty() && envConfiguration != null) {
            logger.warn(
                "Unable to detect Kotlin framework build type for CONFIGURATION=$envConfiguration automatically. " +
                        "Specify 'KOTLIN_FRAMEWORK_BUILD_TYPE' to 'debug' or 'release'"
            )
        } else {
            logger.debug(
                "Not registering $frameworkTaskName, since not called from Xcode " +
                        "('SDK_NAME', 'ARCHS' and 'CONFIGURATION' not provided)"
            )
        }
        return null
    }

    if (frameworkBuildType != envBuildType || !envTargets.contains(frameworkTarget.konanTarget)) return null

    return if (needFatFramework) {
        locateOrRegisterTask<FatFrameworkTask>(frameworkTaskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Packs $frameworkBuildType fat framework for Xcode"
            task.baseName = framework.baseName
            task.destinationDir = appleFrameworkDir(envFrameworkSearchDir)
        }.also {
            it.configure { task -> task.from(framework) }
        }
    } else {
        registerTask<Copy>(frameworkTaskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
            task.dependsOn(framework.linkTaskName)
            task.from(framework.outputDirectory)
            task.into(appleFrameworkDir(envFrameworkSearchDir))
        }
    }
}

internal fun Project.registerEmbedAndSignAppleFrameworkTask(framework: Framework) {
    val assembleTask = registerAssembleAppleFrameworkTask(framework) ?: return

    val envBuildType = XcodeEnvironment.buildType
    val envTargets = XcodeEnvironment.targets
    val envEmbeddedFrameworksDir = XcodeEnvironment.embeddedFrameworksDir
    val envFrameworkSearchDir = XcodeEnvironment.frameworkSearchDir
    val envSign = XcodeEnvironment.sign

    val frameworkTaskName = lowerCamelCaseName("embedAndSign", framework.namePrefix, "AppleFrameworkForXcode")

    if (envBuildType == null || envTargets.isEmpty() || envEmbeddedFrameworksDir == null || envFrameworkSearchDir == null) {
        logger.debug(
            "Not registering $frameworkTaskName, since not called from Xcode " +
                    "('SDK_NAME', 'CONFIGURATION', 'TARGET_BUILD_DIR', 'ARCHS' and 'FRAMEWORKS_FOLDER_PATH' not provided)"
        )
        return
    }

    if (framework.buildType != envBuildType || !envTargets.contains(framework.konanTarget)) return
    if (locateTask<Copy>(frameworkTaskName) != null) return

    registerTask<Copy>(frameworkTaskName) { task ->
        task.group = "build"
        task.description = "Embed and sign ${framework.namePrefix} framework as requested by Xcode's environment variables"

        task.dependsOn(assembleTask)
        task.inputs.apply {
            property("type", envBuildType)
            property("targets", envTargets)
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