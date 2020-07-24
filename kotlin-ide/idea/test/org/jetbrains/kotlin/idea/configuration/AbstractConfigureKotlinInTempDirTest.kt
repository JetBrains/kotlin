/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts

import java.nio.file.Files
import java.nio.file.Path

abstract class AbstractConfigureKotlinInTempDirTest : AbstractConfigureKotlinTest() {
    override fun getProjectDirOrFile(isDirectoryBasedProject: Boolean): Path {
        val projectRoot = super.getProjectDirOrFile(isDirectoryBasedProject)

        val kotlinRuntime = projectRoot.resolve("lib/kotlin-stdlib.jar")
        if (getTestName(true).toLowerCase().contains("latestruntime") && Files.exists(kotlinRuntime)) {
            KotlinArtifacts.instance.kotlinStdlib.copyTo(kotlinRuntime.toFile(), overwrite = true)
        }

        return projectRoot
    }
}