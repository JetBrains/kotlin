/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import java.io.File

/**
 * Creates a dummy framework in the target directory.
 *
 * We represent a Kotlin/Native module to CocoaPods as a vendored framework.
 * CocoaPods needs access to such frameworks during installation process to obtain
 * their type (static or dynamic) and configure the Xcode project accordingly.
 * But we cannot build the real framework before installation because it may
 * depend on CocoaPods libraries which are not downloaded and built at this stage.
 * So we create a dummy static framework to allow CocoaPods install our pod correctly
 * and then replace it with the real one during a real build process.
 */
abstract class DummyFrameworkTask : DefaultTask() {

    @get:Input
    abstract val frameworkName: Property<String>

    @get:Input
    abstract val useStaticFramework: Property<Boolean>

    @get:OutputDirectory
    val outputFramework: Provider<File> = project.provider { project.cocoapodsBuildDirs.dummyFramework }

    private val dummyFrameworkResource: String
        get() {
            val staticOrDynamic = if (!useStaticFramework.get()) "dynamic" else "static"
            return "/cocoapods/$staticOrDynamic/dummy.framework/"
        }

    private fun copyResource(from: String, to: File) {
        to.parentFile.mkdirs()
        to.outputStream().use { file ->
            javaClass.getResourceAsStream(from)!!.use { resource ->
                resource.copyTo(file)
            }
        }
    }

    private fun copyTextResource(from: String, to: File, transform: (String) -> String = { it }) {
        to.parentFile.mkdirs()
        to.printWriter().use { file ->
            javaClass.getResourceAsStream(from)!!.use {
                it.reader().forEachLine { str ->
                    file.println(transform(str))
                }
            }
        }
    }

    private fun copyFrameworkFile(relativeFrom: String, relativeTo: String = relativeFrom) =
        copyResource(
            "$dummyFrameworkResource$relativeFrom",
            outputFramework.get().resolve(relativeTo)
        )

    private fun copyFrameworkTextFile(
        relativeFrom: String,
        relativeTo: String = relativeFrom,
        transform: (String) -> String = { it }
    ) = copyTextResource(
        "$dummyFrameworkResource$relativeFrom",
        outputFramework.get().resolve(relativeTo),
        transform
    )

    @TaskAction
    fun create() {
        // Reset the destination directory
        with(outputFramework.get()) {
            deleteRecursively()
            mkdirs()
        }

        // Copy files for the dummy framework.
        copyFrameworkFile("Info.plist")
        copyFrameworkFile("dummy", frameworkName.get())
        copyFrameworkFile("Headers/placeholder.h")
        copyFrameworkTextFile("Modules/module.modulemap") {
            if (it == "framework module dummy {") {
                it.replace("dummy", frameworkName.get())
            } else {
                it
            }
        }
    }
}
