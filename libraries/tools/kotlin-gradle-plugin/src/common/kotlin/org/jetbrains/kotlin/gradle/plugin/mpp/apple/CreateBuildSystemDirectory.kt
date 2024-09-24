/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.incremental.createDirectory
import java.io.File
import javax.inject.Inject

/**
 * When running Gradle in PreAction scripts we sometimes have to create BUILT_PRODUCTS_DIR if build system didn't create it
 */
@DisableCachingByDefault(because = "This task only copies files")
internal abstract class CreateBuildSystemDirectory @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

    @get:Optional
    @get:Input
    abstract val buildSystemDirectory: Property<File>

    @TaskAction
    fun create() {
        val buildSystemDirectory = this.buildSystemDirectory.orNull ?: return
        if (buildSystemDirectory.exists()) return

        buildSystemDirectory.createDirectory()

        // Mark the directory with this attribute to allow Xcode build system to remove it
        execOperations.exec {
            it.commandLine(
                "/usr/bin/xattr", "-w", "com.apple.xcode.CreatedByBuildSystem", "true", buildSystemDirectory,
            )
        }.assertNormalExitValue()
    }

}