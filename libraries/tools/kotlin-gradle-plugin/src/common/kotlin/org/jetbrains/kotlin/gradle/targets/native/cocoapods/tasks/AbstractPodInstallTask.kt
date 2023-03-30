/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.utils.runCommand
import java.io.File
import java.io.IOException

/**
 * The task takes the path to the Podfile and calls `pod install`
 * to obtain sources or artifacts for the declared dependencies.
 * This task is a part of CocoaPods integration infrastructure.
 */
abstract class AbstractPodInstallTask : CocoapodsTask() {
    init {
        onlyIf { podfile.isPresent }
    }

    @get:Optional
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
        val podInstallCommand = listOf("pod", "install")

        runCommand(podInstallCommand,
                   logger,
                   errorHandler = ::handleError,
                   exceptionHandler = { e: IOException ->
                       CocoapodsErrorHandlingUtil.handle(e, podInstallCommand)
                   },
                   processConfiguration = {
                       directory(workingDir.get())
                   })

        with(podsXcodeProjDirProvider.get()) {
            check(exists() && isDirectory) {
                "The directory 'Pods/Pods.xcodeproj' was not created as a result of the `pod install` call."
            }
        }
    }

    abstract fun handleError(retCode: Int, error: String, process: Process): String?
}

private object CocoapodsErrorHandlingUtil {
    fun handle(e: IOException, command: List<String>) {
        if (e.message?.contains("No such file or directory") == true) {
            val message = """ 
               |'${command.take(2).joinToString(" ")}' command failed with an exception:
               | ${e.message}
               |        
               |        Full command: ${command.joinToString(" ")}
               |        
               |        Possible reason: CocoaPods is not installed
               |        Please check that CocoaPods v1.10 or above is installed.
               |        
               |        To check CocoaPods version type 'pod --version' in the terminal
               |        
               |        To install CocoaPods execute 'sudo gem install cocoapods'
               |
            """.trimMargin()
            throw IllegalStateException(message)
        } else {
            throw e
        }
    }

}
