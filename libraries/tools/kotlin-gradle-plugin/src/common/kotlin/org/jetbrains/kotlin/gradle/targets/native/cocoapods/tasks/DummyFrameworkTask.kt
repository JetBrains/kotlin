/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.MachO
import org.jetbrains.kotlin.gradle.utils.getFile
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
@DisableCachingByDefault
abstract class DummyFrameworkTask : DefaultTask() {

    @get:Input
    abstract val frameworkName: Property<String>

    @get:Input
    abstract val useStaticFramework: Property<Boolean>

    @get:OutputDirectory
    abstract val outputFramework: DirectoryProperty

    @get:Internal
    @Deprecated("Use outputFramework", replaceWith = ReplaceWith("outputFramework.get().asFile"))
    val destinationDir: File
        get() = outputFramework.getFile()

    private val linkageName: String
        get() = if (useStaticFramework.get()) "static" else "dynamic"

    private val dummyFrameworkResource: String
        get() = "/cocoapods/$linkageName/dummy.framework/"

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
            outputFramework.getFile().resolve(relativeTo)
        )

    private fun copyFrameworkTextFile(
        relativeFrom: String,
        relativeTo: String = relativeFrom,
        transform: (String) -> String = { it },
    ) = copyTextResource(
        "$dummyFrameworkResource$relativeFrom",
        outputFramework.getFile().resolve(relativeTo),
        transform
    )

    private fun copyFramework() {
        // Reset the destination directory
        with(outputFramework.getFile()) {
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


    @TaskAction
    fun create() {
        val framework = outputFramework.getFile()
        val binary = framework.resolve(frameworkName.get())

        return when {
            !binary.exists() -> {
                logger.info("Generating dummy-framework because the framework is missing")
                copyFramework()
            }
            MachO.isDylib(binary, logger) == !useStaticFramework.get() -> {
                logger.info("Skipping dummy-framework generation because a $linkageName framework is already present")
            }
            else -> {
                logger.info("Regenerating dummy-framework because present framework has different linkage")
                copyFramework()
            }
        }
    }
}
