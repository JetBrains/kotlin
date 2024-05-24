/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.FrameworkCopy.Companion.dsymFile
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.registerSwiftExportTask
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForBinariesCompilation
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.mapToFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import javax.inject.Inject

@Suppress("ConstPropertyName")
internal object AppleXcodeTasks {
    const val embedAndSignTaskPrefix = "embedAndSign"
    const val embedAndSignTaskPostfix = "AppleFrameworkForXcode"
    const val checkSandboxAndWriteProtection = "checkSandboxAndWriteProtection"
    const val builtProductsDir = "builtProductsDir"
}

private fun Project.registerAssembleAppleFrameworkTask(framework: Framework, environment: XcodeEnvironment): TaskProvider<out Task>? {
    if (!framework.konanTarget.family.isAppleFamily || !framework.konanTarget.enabledOnCurrentHostForBinariesCompilation()) return null

    val envTargets = environment.targets
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

    val envBuildType = environment.buildType

    if (envBuildType == null || envTargets.isEmpty() || environment.builtProductsDir == null) {
        val envConfiguration = System.getenv("CONFIGURATION")
        if (envTargets.isNotEmpty() && envConfiguration != null) {
            project.reportDiagnostic(KotlinToolingDiagnostics.UnknownAppleFrameworkBuildType(envConfiguration))
        } else {
            logger.debug("Not registering $frameworkTaskName, since not called from Xcode")
        }
        return null
    }

    val symbolicLinkTask = registerSymbolicLinkTask(
        frameworkCopyTaskName = frameworkTaskName,
        builtProductsDir = builtProductsDir(frameworkTaskName, environment),
    )

    if (!isRequestedFramework) {
        return locateOrRegisterTask<DefaultTask>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
            task.isEnabled = false
        }
    }

    val frameworkPath: Provider<File>
    val dsymPath: Provider<File>

    val assembleFrameworkTask = when {
        needFatFramework -> locateOrRegisterTask<FatFrameworkTask>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType fat framework for Xcode"
            task.baseName = framework.baseName
            task.destinationDirProperty.fileProvider(appleFrameworkDir(frameworkTaskName, environment))
            task.isEnabled = !project.kotlinPropertiesProvider.swiftExportEnabled && frameworkBuildType == envBuildType
            task.dependsOn(symbolicLinkTask)
        }.also { taskProvider ->
            taskProvider.configure { task -> task.from(framework) }
            frameworkPath = taskProvider.map { it.fatFramework }
            dsymPath = taskProvider.map { it.frameworkLayout.dSYM.rootDir }
        }
        else -> registerTask<FrameworkCopy>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
            task.isEnabled = !project.kotlinPropertiesProvider.swiftExportEnabled && frameworkBuildType == envBuildType
            task.sourceFramework.fileProvider(framework.linkTaskProvider.map { it.outputFile.get() })
            task.sourceDsym.fileProvider(dsymFile(task.sourceFramework.mapToFile()))
            task.destinationDirectory.fileProvider(appleFrameworkDir(frameworkTaskName, environment))
            task.dependsOn(symbolicLinkTask)
        }.also { taskProvider ->
            frameworkPath = taskProvider.map { it.destinationFramework }
            dsymPath = taskProvider.map { it.destinationDsym }
        }
    }

    symbolicLinkTask.configure {
        it.frameworkPath.set(frameworkPath)
        it.dsymPath.set(dsymPath)
        it.shouldDsymLinkExist.set(!framework.isStatic)
    }

    return assembleFrameworkTask
}

private fun Project.registerSymbolicLinkTask(
    frameworkCopyTaskName: String,
    builtProductsDir: Provider<File>,
): TaskProvider<SymbolicLinkToFrameworkTask> {
    return locateOrRegisterTask<SymbolicLinkToFrameworkTask>(
        lowerCamelCaseName(
            "symbolicLinkTo",
            frameworkCopyTaskName
        )
    ) {
        it.enabled = kotlinPropertiesProvider.appleCreateSymbolicLinkToFrameworkInBuiltProductsDir
        it.builtProductsDirectory.set(builtProductsDir)
    }
}

private fun fireEnvException(frameworkTaskName: String, environment: XcodeEnvironment): Nothing {
    val envBuildType = environment.buildType
    val envConfiguration = System.getenv("CONFIGURATION")
    if (envConfiguration != null && envBuildType == null) {
        throw IllegalStateException(
            "Unable to detect Kotlin framework build type for CONFIGURATION=$envConfiguration automatically. " +
                    "Specify 'KOTLIN_FRAMEWORK_BUILD_TYPE' to 'debug' or 'release'" +
                    "\n$environment"
        )
    } else {
        throw IllegalStateException(
            "Please run the $frameworkTaskName task from Xcode " +
                    "('SDK_NAME', 'CONFIGURATION', 'TARGET_BUILD_DIR', 'ARCHS' and 'FRAMEWORKS_FOLDER_PATH' not provided)" +
                    "\n$environment"
        )
    }
}

private fun fireSandboxException(frameworkTaskName: String, userScriptSandboxingEnabled: Boolean) {
    val message = if (userScriptSandboxingEnabled) "You " else "BUILT_PRODUCTS_DIR is not accessible, probably you "
    throw IllegalStateException(
        message +
                "have sandboxing for user scripts enabled." +
                "\nTo make the $frameworkTaskName task pass, disable this feature. " +
                "\nIn your Xcode project, navigate to \"Build Setting\", " +
                "and under \"Build Options\" set \"User script sandboxing\" (ENABLE_USER_SCRIPT_SANDBOXING) to \"NO\". " +
                "\nThen, run \"./gradlew --stop\" to stop the Gradle daemon" +
                "\nFor more information, see documentation: https://jb.gg/ltd9e6"
    )
}

private enum class DirAccessibility {
    ACCESSIBLE,
    NOT_ACCESSIBLE,
    DOES_NOT_EXIST
}

private fun builtProductsDirAccessibility(builtProductsDir: File?): DirAccessibility {
    return if (builtProductsDir != null) {
        try {
            Files.createDirectories(builtProductsDir.toPath())
            val tempFile = File.createTempFile("sandbox", ".tmp", builtProductsDir)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            DirAccessibility.ACCESSIBLE
        } catch (e: IOException) {
            DirAccessibility.NOT_ACCESSIBLE
        }
    } else {
        DirAccessibility.DOES_NOT_EXIST
    }
}

internal fun Project.registerEmbedAndSignAppleFrameworkTask(framework: Framework, environment: XcodeEnvironment) {
    val envBuildType = environment.buildType
    val envTargets = environment.targets
    val envEmbeddedFrameworksDir = environment.embeddedFrameworksDir
    val envSign = environment.sign
    val userScriptSandboxingEnabled = environment.userScriptSandboxingEnabled

    val frameworkTaskName = framework.embedAndSignTaskName()

    if (envBuildType == null || envTargets.isEmpty() || envEmbeddedFrameworksDir == null) {
        locateOrRegisterTask<DefaultTask>(frameworkTaskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Embed and sign ${framework.namePrefix} framework as requested by Xcode's environment variables"
            task.doFirst {
                fireEnvException(frameworkTaskName, environment)
            }
        }
        return
    }

    val checkSandboxAndWriteProtectionTask = locateOrRegisterTask<DefaultTask>(AppleXcodeTasks.checkSandboxAndWriteProtection) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Check BUILT_PRODUCTS_DIR accessible and ENABLE_USER_SCRIPT_SANDBOXING not enabled"
        task.inputs.property(AppleXcodeTasks.builtProductsDir, environment.builtProductsDir)

        task.doFirst {
            val dirAccessible = builtProductsDirAccessibility(it.inputs.properties[AppleXcodeTasks.builtProductsDir] as File?)
            when (dirAccessible) {
                DirAccessibility.NOT_ACCESSIBLE -> fireSandboxException(frameworkTaskName, userScriptSandboxingEnabled)
                DirAccessibility.DOES_NOT_EXIST,
                DirAccessibility.ACCESSIBLE,
                -> if (userScriptSandboxingEnabled) {
                    fireSandboxException(frameworkTaskName, true)
                }
            }
        }
    }

    val embedAndSignTask = locateOrRegisterTask<EmbedAndSignTask>(frameworkTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Embed and sign ${framework.namePrefix} framework as requested by Xcode's environment variables"
        task.isEnabled = !(project.kotlinPropertiesProvider.swiftExportEnabled || framework.isStatic)
        task.inputs.apply {
            property("type", envBuildType)
            property("targets", envTargets)
            property("embeddedFrameworksDir", envEmbeddedFrameworksDir)
            property("userScriptSandboxingEnabled", userScriptSandboxingEnabled)
            if (envSign != null) {
                property("sign", envSign)
            }
        }
    }

    val swiftExportTask: TaskProvider<*>? =
        if (project.kotlinPropertiesProvider.swiftExportEnabled &&
            environment.targets.contains(framework.konanTarget) &&
            framework.buildType == envBuildType
        ) {
            registerSwiftExportTask(framework).apply {
                dependsOn(checkSandboxAndWriteProtectionTask)
            }
        } else {
            null
        }

    val assembleTask = registerAssembleAppleFrameworkTask(framework, environment)?.apply {
        dependsOn(checkSandboxAndWriteProtectionTask)
    } ?: return

    if (framework.buildType != envBuildType || !envTargets.contains(framework.konanTarget)) return

    embedAndSignTask.configure { task ->
        val frameworkFile = framework.outputFile
        if (swiftExportTask != null) {
            task.dependsOn(swiftExportTask)
        } else {
            task.dependsOn(assembleTask)
        }
        task.sourceFramework.fileProvider(appleFrameworkDir(frameworkTaskName, environment).map { it.resolve(frameworkFile.name) })
        task.destinationDirectory.set(envEmbeddedFrameworksDir)
        if (envSign != null) {
            task.doLast {
                val binary = envEmbeddedFrameworksDir
                    .resolve(frameworkFile.name)
                    .resolve(frameworkFile.nameWithoutExtension)
                task.execOperations.exec {
                    it.commandLine("codesign", "--force", "--sign", envSign, "--", binary)
                }
            }
        }
    }
}

private fun Framework.embedAndSignTaskName(): String = lowerCamelCaseName(
    AppleXcodeTasks.embedAndSignTaskPrefix,
    namePrefix,
    AppleXcodeTasks.embedAndSignTaskPostfix
)

private val Framework.namePrefix: String
    get() = KotlinNativeBinaryContainer.extractPrefixFromBinaryName(
        name,
        buildType,
        outputKind.taskNameClassifier
    )

/**
 * [XcodeEnvironment.builtProductsDir] if not disabled.
 *
 * Or if [XcodeEnvironment.frameworkSearchDir] is absolute use it, otherwise make it relative to buildDir/xcode-frameworks
 */
private fun Project.appleFrameworkDir(frameworkTaskName: String, environment: XcodeEnvironment): Provider<File> {
    return layout.buildDirectory.dir("xcode-frameworks").map {
        it.asFile.resolve(environment.frameworkSearchDir ?: fireEnvException(frameworkTaskName, environment))
    }
}

private fun Project.builtProductsDir(frameworkTaskName: String, environment: XcodeEnvironment) = project.provider {
    environment.builtProductsDir ?: fireEnvException(frameworkTaskName, environment)
}


/**
 * macOS frameworks contain symlinks which are resolved/removed by the Gradle [Copy] task.
 * To preserve these symlinks we are using the `cp` command instead.
 * See https://youtrack.jetbrains.com/issue/KT-48594.
 */
@DisableCachingByDefault(because = "Caching breaks symlinks inside frameworks")
internal abstract class FrameworkCopy : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    abstract val sourceFramework: DirectoryProperty

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    @get:Optional
    @get:IgnoreEmptyDirectories
    abstract val sourceDsym: DirectoryProperty

    @get:OutputDirectory
    abstract val destinationDirectory: DirectoryProperty

    @get:Internal
    internal val destinationFramework get() = destinationDirectory.getFile().resolve(sourceFramework.getFile().name)

    @get:Internal
    internal val destinationDsym get() = destinationDirectory.getFile().resolve(sourceDsym.getFile().name)

    @TaskAction
    open fun copy() {
        copy(sourceFramework.getFile(), destinationFramework)
        if (sourceDsym.isPresent && sourceDsym.getFile().exists()) {
            copy(sourceDsym.getFile(), destinationDsym)
        }
    }

    private fun copy(
        source: File,
        destination: File,
    ) {
        if (destination.exists()) {
            execOperations.exec { it.commandLine("rm", "-r", destination.absolutePath) }
        }
        execOperations.exec { it.commandLine("cp", "-R", source.absolutePath, destination.absolutePath) }
    }

    companion object {
        fun dsymFile(framework: Provider<File>): Provider<File> = framework.map { File(it.path + ".dSYM") }
    }
}

@DisableCachingByDefault(because = "Caching breaks symlinks inside frameworks")
internal abstract class EmbedAndSignTask : FrameworkCopy()
