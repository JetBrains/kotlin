/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

internal object AppleXcodeTasks {
    const val embedAndSignTaskPrefix = "embedAndSign"
    const val embedAndSignTaskPostfix = "AppleFrameworkForXcode"
}

private object XcodeEnvironment {
    val buildType: NativeBuildType?
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

    override fun toString() = """
        XcodeEnvironment:
          buildType=$buildType
          targets=$targets
          frameworkSearchDir=$frameworkSearchDir
          embeddedFrameworksDir=$embeddedFrameworksDir
          sign=$sign
    """.trimIndent()
}

private fun Project.registerAssembleAppleFrameworkTask(framework: Framework): TaskProvider<out Task>? {
    if (!framework.konanTarget.family.isAppleFamily || !framework.konanTarget.enabledOnCurrentHost) return null

    val envTargets = XcodeEnvironment.targets
    val needFatFramework = envTargets.size > 1

    val frameworkBuildType = framework.buildType
    val frameworkTarget = framework.target
    val isRequestedFramework = envTargets.contains(frameworkTarget.konanTarget)

    val frameworkTaskName = lowerCamelCaseName(
        "assemble",
        framework.namePrefix,
        frameworkBuildType.getName(),
        "AppleFrameworkForXcode",
        if (isRequestedFramework && needFatFramework) null else frameworkTarget.name //for fat framework we need common name
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
            logger.debug("Not registering $frameworkTaskName, since not called from Xcode")
        }
        return null
    }

    return when {
        !isRequestedFramework -> locateOrRegisterTask<DefaultTask>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
            task.isEnabled = false
        }
        needFatFramework -> locateOrRegisterTask<FatFrameworkTask>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType fat framework for Xcode"
            task.baseName = framework.baseName
            task.destinationDir = appleFrameworkDir(envFrameworkSearchDir)
            task.isEnabled = frameworkBuildType == envBuildType
        }.also {
            it.configure { task -> task.from(framework) }
        }
        else -> registerTask<FrameworkCopy>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
            task.isEnabled = frameworkBuildType == envBuildType
            task.dependsOn(framework.linkTaskName)
            task.files = files({ framework.outputDirectory.listFiles() })
            task.destDir = appleFrameworkDir(envFrameworkSearchDir)
        }
    }
}

internal fun Project.registerEmbedAndSignAppleFrameworkTask(framework: Framework) {
    val envBuildType = XcodeEnvironment.buildType
    val envTargets = XcodeEnvironment.targets
    val envEmbeddedFrameworksDir = XcodeEnvironment.embeddedFrameworksDir
    val envFrameworkSearchDir = XcodeEnvironment.frameworkSearchDir
    val envSign = XcodeEnvironment.sign

    val frameworkTaskName = lowerCamelCaseName(AppleXcodeTasks.embedAndSignTaskPrefix, framework.namePrefix, AppleXcodeTasks.embedAndSignTaskPostfix)

    if (envBuildType == null || envTargets.isEmpty() || envEmbeddedFrameworksDir == null || envFrameworkSearchDir == null) {
        locateOrRegisterTask<DefaultTask>(frameworkTaskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Embed and sign ${framework.namePrefix} framework as requested by Xcode's environment variables"
            task.doFirst {
                val envConfiguration = System.getenv("CONFIGURATION")
                if (envConfiguration != null && envBuildType == null) {
                    throw IllegalStateException(
                        "Unable to detect Kotlin framework build type for CONFIGURATION=$envConfiguration automatically. " +
                                "Specify 'KOTLIN_FRAMEWORK_BUILD_TYPE' to 'debug' or 'release'" +
                                "\n$XcodeEnvironment"
                    )
                } else {
                    throw IllegalStateException(
                        "Please run the $frameworkTaskName task from Xcode " +
                                "('SDK_NAME', 'CONFIGURATION', 'TARGET_BUILD_DIR', 'ARCHS' and 'FRAMEWORKS_FOLDER_PATH' not provided)" +
                                "\n$XcodeEnvironment"
                    )
                }
            }
        }
        return
    }

    val embedAndSignTask = locateOrRegisterTask<FrameworkCopy>(frameworkTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Embed and sign ${framework.namePrefix} framework as requested by Xcode's environment variables"
        task.isEnabled = !framework.isStatic
        task.inputs.apply {
            property("type", envBuildType)
            property("targets", envTargets)
            property("embeddedFrameworksDir", envEmbeddedFrameworksDir)
            if (envSign != null) {
                property("sign", envSign)
            }
        }
    }

    val assembleTask = registerAssembleAppleFrameworkTask(framework) ?: return
    if (framework.buildType != envBuildType || !envTargets.contains(framework.konanTarget)) return

    embedAndSignTask.configure { task ->
        task.dependsOn(assembleTask)
        task.files = files(File(appleFrameworkDir(envFrameworkSearchDir), framework.outputFile.name))
        task.destDir = envEmbeddedFrameworksDir
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

/**
 * macOS frameworks contain symlinks which are resolved/removed by the Gradle [Copy] task.
 * To preserve these symlinks we are using the `cp` command instead.
 * See https://youtrack.jetbrains.com/issue/KT-48594.
 */
internal abstract class FrameworkCopy : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract var files: FileCollection

    @get:OutputDirectory
    abstract var destDir: File

    @TaskAction
    fun copy() {
        destDir.mkdirs()
        files.forEach { file ->
            File(destDir, file.name).let destFile@{ destFile ->
                if (!destFile.exists()) return@destFile
                project.exec { it.commandLine("rm", "-r", destFile.absolutePath) }
            }
            project.exec { it.commandLine("cp", "-R", file.absolutePath, destDir.absolutePath) }
        }
    }
}
