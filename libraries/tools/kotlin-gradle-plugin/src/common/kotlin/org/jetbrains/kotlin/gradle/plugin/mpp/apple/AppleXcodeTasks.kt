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
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary
import org.jetbrains.kotlin.gradle.plugin.mpp.StaticLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.FrameworkCopy.Companion.dsymFile
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportDSLConstants
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.registerSwiftExportTask
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForBinariesCompilation
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.mapToFile
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import java.io.File
import javax.inject.Inject

@Suppress("ConstPropertyName")
internal object AppleXcodeTasks {
    const val embedAndSignTaskPrefix = "embedAndSign"
    const val embedAndSignTaskPostfix = "AppleFrameworkForXcode"
    const val checkSandboxAndWriteProtection = "checkSandboxAndWriteProtection"
}

private fun Project.registerAssembleAppleFrameworkTask(framework: Framework, environment: XcodeEnvironment): TaskProvider<out Task>? {
    if (!framework.konanTarget.family.isAppleFamily || !framework.konanTarget.enabledOnCurrentHostForBinariesCompilation()) return null

    val envTargets = environment.targets
    val needFatFramework = envTargets.size > 1
    val envBuildType = environment.buildType

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

    if (!shouldRegisterEmbedTask(environment, frameworkTaskName)) {
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
            task.isEnabled = frameworkBuildType == envBuildType
            task.dependsOn(symbolicLinkTask)
        }.also { taskProvider ->
            taskProvider.configure { task -> task.from(framework) }
            frameworkPath = taskProvider.map { it.fatFramework }
            dsymPath = taskProvider.map { it.frameworkLayout.dSYM.rootDir }
        }
        else -> registerTask<FrameworkCopy>(frameworkTaskName) { task ->
            task.description = "Packs $frameworkBuildType ${frameworkTarget.name} framework for Xcode"
            task.isEnabled = frameworkBuildType == envBuildType
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

@ExperimentalSwiftExportDsl
internal fun Project.registerEmbedSwiftExportTask(
    binary: StaticLibrary,
    environment: XcodeEnvironment,
    swiftExportExtension: SwiftExportExtension,
) {
    val envTargets = environment.targets
    val envBuildType = environment.buildType
    val isMatchingBinary = envTargets.contains(binary.konanTarget) && binary.buildType == envBuildType
    val binaryTaskName = binary.embedSwiftExportTaskName()

    if (isMatchingBinary) {
        if (!isRunWithXcodeEnvironment(
                environment,
                binaryTaskName,
                "Embed swift export ${binary.namePrefix} library as requested by Xcode's environment variables"
            )
        ) {
            return
        }

        val swiftExportTask = registerSwiftExportTask(
            swiftExportExtension,
            SwiftExportDSLConstants.TASK_GROUP,
            binary
        ).apply {
            dependsOn(checkSandboxAndWriteProtectionTask(environment, environment.userScriptSandboxingEnabled))
        }

        registerEmbedTask(binary, binaryTaskName, environment, swiftExportTask) { false }
    }
}

internal fun Project.registerEmbedAndSignAppleFrameworkTask(framework: Framework, environment: XcodeEnvironment) {
    val frameworkTaskName = framework.embedAndSignTaskName()

    if (!isRunWithXcodeEnvironment(
            environment,
            frameworkTaskName,
            "Embed and sign ${framework.namePrefix} framework as requested by Xcode's environment variables"
        )
    ) {
        return
    }

    val assembleTask = registerAssembleAppleFrameworkTask(framework, environment)?.apply {
        dependsOn(checkSandboxAndWriteProtectionTask(environment, environment.userScriptSandboxingEnabled))
    } ?: return

    registerEmbedTask(framework, frameworkTaskName, environment, assembleTask) { !framework.isStatic }
}

private fun Project.isRunWithXcodeEnvironment(
    environment: XcodeEnvironment,
    taskName: String,
    taskDescription: String,
): Boolean {
    val envBuildType = environment.buildType
    val envTargets = environment.targets
    val envEmbeddedFrameworksDir = environment.embeddedFrameworksDir

    if (envBuildType == null || envTargets.isEmpty() || envEmbeddedFrameworksDir == null) {
        locateOrRegisterTask<DefaultTask>(taskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = taskDescription
            task.doFirst {
                fireEnvException(taskName, environment)
            }
        }

        return false
    }

    return true
}

private fun Project.registerEmbedTask(
    binary: NativeBinary,
    frameworkTaskName: String,
    environment: XcodeEnvironment,
    dependencyTask: TaskProvider<out Task>,
    embedAndSignEnabled: () -> Boolean = { true },
) {
    val envBuildType = environment.buildType
    val envTargets = environment.targets
    val envEmbeddedFrameworksDir = environment.embeddedFrameworksDir
    val envSign = environment.sign
    val userScriptSandboxingEnabled = environment.userScriptSandboxingEnabled

    if (envBuildType == null || envTargets.isEmpty() || envEmbeddedFrameworksDir == null) return

    val embedAndSignTask = locateOrRegisterTask<EmbedAndSignTask>(frameworkTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Embed and sign ${binary.namePrefix} framework as requested by Xcode's environment variables"
        task.isEnabled = embedAndSignEnabled()
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

    if (binary.buildType != envBuildType || !envTargets.contains(binary.konanTarget)) return

    embedAndSignTask.configure { task ->
        val frameworkFile = binary.outputFile
        task.dependsOn(dependencyTask)
        task.sourceFramework.fileProvider(appleFrameworkDir(frameworkTaskName, environment).map { it.resolve(frameworkFile.name) })
        task.destinationDirectory.set(envEmbeddedFrameworksDir)
        if (envSign != null) {
            task.doLast {
                val binaryToSign = envEmbeddedFrameworksDir
                    .resolve(frameworkFile.name)
                    .resolve(frameworkFile.nameWithoutExtension)
                task.execOperations.exec {
                    it.commandLine("codesign", "--force", "--sign", envSign, "--", binaryToSign)
                }
            }
        }
    }
}

private fun Project.checkSandboxAndWriteProtectionTask(
    environment: XcodeEnvironment,
    userScriptSandboxingEnabled: Boolean,
) =
    locateOrRegisterTask<CheckSandboxAndWriteProtectionTask>(AppleXcodeTasks.checkSandboxAndWriteProtection) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Check BUILT_PRODUCTS_DIR accessible and ENABLE_USER_SCRIPT_SANDBOXING not enabled"

        task.builtProductsDir.set(environment.builtProductsDir)
        task.userScriptSandboxingEnabled.set(userScriptSandboxingEnabled)
    }

private fun Project.shouldRegisterEmbedTask(environment: XcodeEnvironment, frameworkTaskName: String): Boolean {
    val envBuildType = environment.buildType
    val envTargets = environment.targets

    if (envBuildType == null || envTargets.isEmpty() || environment.builtProductsDir == null) {
        val envConfiguration = System.getenv("CONFIGURATION")
        if (envTargets.isNotEmpty() && envConfiguration != null) {
            project.reportDiagnostic(KotlinToolingDiagnostics.UnknownAppleFrameworkBuildType(envConfiguration))
        } else {
            logger.debug("Not registering $frameworkTaskName, since not called from Xcode")
        }
        return false
    }

    return true
}

private fun NativeBinary.embedAndSignTaskName(): String = lowerCamelCaseName(
    AppleXcodeTasks.embedAndSignTaskPrefix,
    namePrefix,
    AppleXcodeTasks.embedAndSignTaskPostfix
)

private fun NativeBinary.embedSwiftExportTaskName(): String = lowerCamelCaseName(
    "embed",
    namePrefix.let { it.ifBlank { SwiftExportDSLConstants.SWIFT_EXPORT_LIBRARY_PREFIX } },
    "ForXcode"
)

private val NativeBinary.namePrefix: String
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
