/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Base task providing helper methods for Xcode command execution.
 */
abstract class AbstractXcodeTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    protected fun runCommand(vararg args: String): String {
        val stdout = ByteArrayOutputStream()
        val result = execOperations.exec {
            it.commandLine(*args)
            it.standardOutput = stdout
        }
        if (result.exitValue != 0) {
            throw GradleException("Command failed: ${args.joinToString(" ")}")
        }
        return stdout.toString().trim()
    }
}