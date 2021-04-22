/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
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

            //todo support simulator architecture for Apple Silicon
            return when {
                sdk.startsWith("iphoneos") -> KonanTarget.IOS_ARM64
                sdk.startsWith("iphonesimulator") -> KonanTarget.IOS_X64
                sdk.startsWith("watchos") -> KonanTarget.WATCHOS_ARM64
                sdk.startsWith("watchsimulator") -> KonanTarget.WATCHOS_X64
                sdk.startsWith("appletvos") -> KonanTarget.TVOS_ARM64
                sdk.startsWith("appletvsimulator") -> KonanTarget.TVOS_X64
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

internal fun Project.registerAssembleAppleFrameworkTask(framework: Framework) {
    if (!framework.konanTarget.family.isAppleFamily || !framework.konanTarget.enabledOnCurrentHost) return

    val frameworkBuildType = framework.buildType
    val frameworkTarget = framework.target
    val frameworkTaskName = lowerCamelCaseName(
        "assemble",
        framework.baseName,
        frameworkBuildType.name.toLowerCaseAsciiOnly(),
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
        return
    }

    if (frameworkBuildType != envBuildType || frameworkTarget.konanTarget != envTarget) return

    umbrellaAssembleAppleFrameworkTask.dependsOn(
        registerTask<Copy>(frameworkTaskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
            task.dependsOn(framework.linkTaskName)
            task.from(framework.outputDirectory)
            task.into(appleFrameworkDir(envFrameworkSearchDir))
        }
    )
}

private const val UMBRELLA_ASSEMBLE_APPLE_FRAMEWORK = "assembleAppleFrameworkForXcode"
private val Project.umbrellaAssembleAppleFrameworkTask: TaskProvider<Task>
    get() = locateOrRegisterTask(UMBRELLA_ASSEMBLE_APPLE_FRAMEWORK) {
        it.group = "build"
        it.description = "Build all frameworks as requested by Xcode's environment variables"
    }

private const val EMBED_AND_SIGN_APPLE_FRAMEWORK = "embedAndSignAppleFrameworkForXcode"
internal fun Project.registerEmbedAndSignAppleFrameworkTask() {
    val envBuildType = XcodeEnvironment.buildType
    val envTarget = XcodeEnvironment.target
    val envEmbeddedFrameworksDir = XcodeEnvironment.embeddedFrameworksDir
    val envFrameworkSearchDir = XcodeEnvironment.frameworkSearchDir
    val envSign = XcodeEnvironment.sign

    if (envBuildType == null || envTarget == null || envEmbeddedFrameworksDir == null || envFrameworkSearchDir == null) {
        logger.debug(
            "Not registering $EMBED_AND_SIGN_APPLE_FRAMEWORK, since not called from Xcode " +
                    "('SDK_NAME', 'CONFIGURATION', 'TARGET_BUILD_DIR' and 'FRAMEWORKS_FOLDER_PATH' not provided)"
        )
        return
    }

    registerTask<Copy>(EMBED_AND_SIGN_APPLE_FRAMEWORK) { task ->
        task.group = "build"
        task.description = "Embed and sign all frameworks as requested by Xcode's environment variables"

        val appleTargets: NamedDomainObjectCollection<KotlinNativeTarget>? =
            multiplatformExtensionOrNull?.targets
                ?.withType(KotlinNativeTarget::class.java)
                ?.matching { it.konanTarget == envTarget }

        task.dependsOn(UMBRELLA_ASSEMBLE_APPLE_FRAMEWORK)
        task.inputs.apply {
            property("type", envBuildType)
            property("target", envTarget)
            property("embeddedFrameworksDir", envEmbeddedFrameworksDir)
            property("sign", envSign)
        }

        task.onlyIf { !appleTargets.isNullOrEmpty() }

        task.from(appleFrameworkDir(envFrameworkSearchDir)) { spec ->
            appleTargets?.all { target ->
                target.binaries.withType(Framework::class.java)
                    .matching { f -> f.buildType == envBuildType }
                    .all { framework ->
                        spec.include(framework.outputFile.name + "/**")
                    }
            }
        }

        task.into(envEmbeddedFrameworksDir)

        if (envSign != null) {
            task.doLast {
                appleTargets?.all { target ->
                    target.binaries.withType(Framework::class.java)
                        .matching { f -> f.buildType == envBuildType }
                        .all { framework ->
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
    }
}

private fun Project.appleFrameworkDir(frameworkSearchDir: File) =
    buildDir.resolve("xcode-frameworks").resolve(frameworkSearchDir)