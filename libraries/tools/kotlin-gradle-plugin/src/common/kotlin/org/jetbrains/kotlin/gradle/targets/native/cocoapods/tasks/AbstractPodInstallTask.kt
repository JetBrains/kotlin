/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.onlyIfCompat
import org.jetbrains.kotlin.gradle.utils.runCommand
import java.io.File
import java.io.IOException

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
        // env is used here to work around the JVM PATH caching when spawning a child process with custom environment, i.e. LC_ALL
        // The caching causes the ProcessBuilder to ignore changes in the PATH that may occur on incremental runs of the Gradle daemon
        // KT-60394
        val podInstallCommand = listOf("env", "pod", "install")

        runCommand(podInstallCommand,
                   logger,
                   errorHandler = { retCode, output, process -> sharedHandleError(podInstallCommand, retCode, output, process) },
                   processConfiguration = {
                       directory(workingDir.get())
                       // CocoaPods requires to be run with Unicode external encoding
                       environment().putIfAbsent("LC_ALL", "en_US.UTF-8")
                   })

        with(podsXcodeProjDirProvider.get()) {
            check(exists() && isDirectory) {
                "The directory 'Pods/Pods.xcodeproj' was not created as a result of the `pod install` call."
            }
        }
    }

    private fun sharedHandleError(podInstallCommand: List<String>, retCode: Int, error: String, process: Process): String? {
        return if (error.contains("No such file or directory")) {
            val command = podInstallCommand.joinToString(" ")
            """ 
               |'$command' command failed with an exception:
               | $error
               |        
               |        Full command: $command
               |        
               |        Possible reason: CocoaPods is not installed
               |        Please check that CocoaPods v1.10 or above is installed.
               |        
               |        To check CocoaPods version type 'pod --version' in the terminal
               |        
               |        To install CocoaPods execute 'sudo gem install cocoapods'
               |
            """.trimMargin()
        } else {
            handleError(retCode, error, process)
        }
    }

    abstract fun handleError(retCode: Int, error: String, process: Process): String?
}