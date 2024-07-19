/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import java.io.File
import java.io.IOException
import java.nio.file.Files

@DisableCachingByDefault(because = "We are checking only file permissions")
internal abstract class CheckSandboxAndWriteProtectionTask : DefaultTask(), UsesKotlinToolingDiagnostics {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val builtProductsDir: Property<File>

    @get:Input
    abstract val userScriptSandboxingEnabled: Property<Boolean>

    @get:Input
    abstract val taskName: Property<String>

    @TaskAction
    fun checkSandboxAndWriteProtection() {
        val dirAccessible = builtProductsDirAccessibility(builtProductsDir.orNull)
        val task = taskName.get()
        val sandboxingEnabled = userScriptSandboxingEnabled.get()

        when (dirAccessible) {
            DirAccessibility.NOT_ACCESSIBLE -> fireSandboxException(
                task,
                sandboxingEnabled
            )
            DirAccessibility.DOES_NOT_EXIST,
            DirAccessibility.ACCESSIBLE,
                -> if (sandboxingEnabled) {
                fireSandboxException(task, true)
            }
        }
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

    private fun fireSandboxException(taskName: String, userScriptSandboxingEnabled: Boolean) {
        toolingDiagnosticsCollector.get().report(
            this, KotlinToolingDiagnostics.XcodeUserScriptSandboxingDiagnostic(
                taskName,
                userScriptSandboxingEnabled
            )
        )
    }
}