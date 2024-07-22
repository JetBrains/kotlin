/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.onlyIfCompat
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.gradle.utils.runCommandWithFallback
import java.io.File
import java.nio.file.Files

/**
 * The task takes the path to the Podfile and calls `pod install`
 * to obtain sources or artifacts for the declared dependencies.
 * This task is a part of CocoaPods integration infrastructure.
 */
@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AbstractPodInstallTask : CocoapodsTask() {
    init {
        onlyIfCompat("Podfile location is set") { podfile.isPresent }
    }

    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val podfile: Property<File?>

    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    abstract val podExecutablePath: RegularFileProperty

    @get:Internal
    protected val workingDir: Provider<File> = podfile.map { file: File? ->
        requireNotNull(file) { "Task outputs shouldn't be queried if it's skipped" }.parentFile
    }

    @get:OutputDirectory
    internal val podsDir: Provider<File> = workingDir.map { it.resolve("Pods") }

    @get:Internal
    internal val podsXcodeProjDirProvider: Provider<File> = podsDir.map { it.resolve("Pods.xcodeproj") }

    @TaskAction
    open fun doPodInstall() {
        runPodInstall()

        with(podsXcodeProjDirProvider.get()) {
            check(exists() && isDirectory) {
                "The directory 'Pods/Pods.xcodeproj' was not created as a result of the `pod install` call."
            }
        }
    }

    private fun podExecutable(): String {
        return when (val podPath = podExecutablePath.orNull?.asFile) {
            is File -> podPath.absolutePath.ifBlank { getPodExecutablePath() }
            else -> getPodExecutablePath()
        }
    }

    private fun getPodExecutablePath(): String {
        val whichPodCommand = listOf("/usr/bin/which", "pod")
        val output = mutableListOf<String>()
        runCommand(
            whichPodCommand,
            logger,
            onStdOutLine = { output.add(it) },
            captureResult = CaptureCommandResult(
                temporaryDirectory = Files.createTempDirectory("AbstractPodInstallTask").toFile().apply { deleteOnExit() },
                onResult = { result ->
                    if (result.retCode != 0) {

                    }
                }
            ) { result ->
//                if (result.retCode == 1) {
//                    missingPodsError()
//                } else {
//                    sharedHandleError(checkPodCommand, result)
//                }
            }
        )
        if (output.isEmpty()) throw IllegalStateException(missingPodsError())
        return output[0]
    }

    private fun runPodInstall() {
        val podInstallCommand = listOf(podExecutable(), "install")

        var isRepoOutOfDate = false
        val result = runPodInstallCommand(
            podInstallCommand,
            onStdOutLine = {
                if (it.contains("out-of-date source repos which you can update with `pod repo update` or with `pod install --repo-update`")) {
                    isRepoOutOfDate = true
                }
            },
            errorOnNonZeroExitCode = false
        )

        if (result.returnCode != 0) {
            if (isRepoOutOfDate) {
                logger.info("Retrying \"pod install\" with --repo-update")
                var errorMessagePrefix =
                runPodInstallCommand(
                    podInstallCommand + listOf("--repo-update"),
                    logger,
                    onStdOutLine = { line ->

                    }
                )
            } else {

            }
        }

        return runCommandWithFallback(podInstallCommand,
                                      logger,
                                      fallback = { result ->
                                          val output = result.stdErr.ifBlank { result.stdOut }
                                          if (output.contains("out-of-date source repos which you can update with `pod repo update` or with `pod install --repo-update`") && updateRepo.not()) {
                                              CommandFallback.Action(runPodInstall(true))
                                          } else {
                                              CommandFallback.Error(sharedHandleError(podInstallCommand, result))
                                          }
                                      },
                                      )
    }

    private fun runPodInstallCommand(
        command: List<String>,
        onStdOutLine: (String) -> Unit,
        errorOnNonZeroExitCode: Boolean,
    ): RunProcessResult = runCommand(
        command,
        logger,
        onStdOutLine = onStdOutLine,
        errorOnNonZeroExitCode = errorOnNonZeroExitCode,
        processConfiguration = {
            directory(workingDir.get())
            // CocoaPods requires to be run with Unicode external encoding
            environment().putIfAbsent("LC_ALL", "en_US.UTF-8")
        }
    )

    private fun sharedHandleError(podInstallCommand: List<String>, result: RunProcessResult): String? {
        val command = podInstallCommand.joinToString(" ")
        val output = result.stdErr.ifBlank { result.stdOut }

        var message = """
            |'$command' command failed with an exception:
            | stdErr: ${result.stdErr}
            | stdOut: ${result.stdOut}
            | exitCode: ${result.returnCode}
            |        
        """.trimMargin()

        if (output.contains("No such file or directory")) {
            message += """ 
               |        Full command: $command
               |        
               |        Possible reason: CocoaPods is not installed
               |        Please check that CocoaPods v1.14 or above is installed.
               |        
               |        To check CocoaPods version type 'pod --version' in the terminal
               |        To install CocoaPods execute 'sudo gem install cocoapods'
               |        For more information, refer to the documentation: https://kotl.in/fx2sde
               |
            """.trimMargin()
            return message
        } else if (output.contains("[Xcodeproj] Unknown object version")) {
            message += """
               |        Your CocoaPods installation may be outdated or corrupted
               |
               |        To update CocoaPods execute 'sudo gem install cocoapods'
               |        For more information, refer to the documentation: https://kotl.in/0xfxux
               |
            """.trimMargin()
            return message
        } else {
            return handleError(result)
        }
    }

    private fun missingPodsError(): String {
        return """
                  |        ERROR: CocoaPods executable not found in your PATH.
                  |        Please make sure CocoaPods is installed on your system.
                  |
                  |        You can install CocoaPods using the following command:
                  |        ${'$'} sudo gem install cocoapods
                  |
                  |        If CocoaPods is already installed and not in your PATH, you can define the
                  |        CocoaPods executable path in the local.properties file by executing the following:
                  |        ${'$'} echo -e "kotlin.apple.cocoapods.bin=${'$'}(which pod)" >> local.properties
                  |
                  |        For more information, refer to the documentation: https://kotl.in/hxxwtk
                  |        
               """.trimMargin()
    }

    abstract fun handleError(result: RunProcessResult): String?
}